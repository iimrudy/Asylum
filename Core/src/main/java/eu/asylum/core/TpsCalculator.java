package eu.asylum.core;

import lombok.Getter;

public class TpsCalculator implements Runnable {

    @Getter
    private static double tps = 0;
    long sec;
    long currentSec;
    int ticks;
    int delay;

    @Override
    public void run() {
        sec = (System.currentTimeMillis() / 1000);

        if (currentSec == sec) {// this code block triggers each tick

            ticks++;
        } else {// this code block triggers each second

            currentSec = sec;
            tps = (tps == 0 ? ticks : ((tps + ticks) / 2));
            ticks = 0;

        }
    }

}