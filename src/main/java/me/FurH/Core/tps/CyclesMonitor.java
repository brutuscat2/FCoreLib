package me.FurH.Core.tps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import me.FurH.Core.CorePlugin;
import me.FurH.Core.number.NumberUtils;
import me.FurH.Core.player.PlayerUtils;
import org.bukkit.Bukkit;

/**
 *
 * @author FurmigaHumana
 * All Rights Reserved unless otherwise explicitly stated.
 */
public class CyclesMonitor {

    private LinkedList<Double> history = new LinkedList<Double>(
            Arrays.asList(new Double[] { 20.0,20.0,20.0,20.0,20.0,20.0,20.0,20.0,20.0,20.0 }));

    private long last_hold = System.currentTimeMillis();
    private static List<ICycleTPS> references;
    
    private long interval = 100;
    private long last = -1;

    public CyclesMonitor(CorePlugin plugin) {
        
        references = new ArrayList<ICycleTPS>();
        
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                last = System.nanoTime();
            }
        }, 300L, interval);
        
        new Timer("FCoreLib TPS Monitor", true)
                .scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                
                if (references.isEmpty()) {
                    return;
                }
                
                long now = System.currentTimeMillis();
                double tps = getCurrentTPS();

                if (tps < 10.0D) {
                    tps = getAverageTPS();
                }

                if (((now - last_hold) > 15000) && tps < 15.0D) {
                    
                    System.out.println("Freezing " + references.size() + " references");
                    
                    for (int j1 = 0; j1 < references.size(); j1++) {
                        ICycleTPS reference = references.get(j1);
                        try {
                            reference.hold();
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                    
                    last_hold = System.currentTimeMillis();
                }
            }
            
        }, 16000, 5000);
    }

    private double getCurrentTPS() {

        long spent = Math.max((System.nanoTime() - last) / 1000, 1);
        double tps = Math.min(interval * 1000000.0 / spent, 20);

        if (history.size() > 10) {
            history.remove();
        }

        history.add(tps);

        return Math.floor(tps);
    }

    public static boolean register(ICycleTPS reference) {
        return references.add(reference);
    }
    
    public double getAverageTPS() {
        return Math.floor(NumberUtils.getInBounds(PlayerUtils.getAverage(history.toArray(new Double[ history.size()  ])), 20, 1));
    }
}