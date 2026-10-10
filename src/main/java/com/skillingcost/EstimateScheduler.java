package com.skillingcost;

import javax.swing.Timer;

interface EstimateScheduler
{
    void restart();
    void stop();

    static EstimateScheduler swing(Runnable action)
    {
        Timer timer = new Timer(250, event -> action.run());
        timer.setRepeats(false);
        return new EstimateScheduler()
        {
            public void restart() { timer.restart(); }
            public void stop() { timer.stop(); }
        };
    }
}
