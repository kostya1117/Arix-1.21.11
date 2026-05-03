package ru.arixcompany.utils.math;

public class Timer {
   private long startTime;
   public long lastMS = System.currentTimeMillis();

   public void reset() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean isReached(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public void setTime(long time) {
      this.lastMS = time;
   }

   public boolean finished(double delay) {
      return System.currentTimeMillis() - delay >= this.startTime;
   }

   public long elapsedTime() {
      return System.currentTimeMillis() - this.startTime;
   }

   public void setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
   }

   public boolean hasReached(double milliseconds) {
      return this.getTimePassed() >= milliseconds;
   }

   public long getTimePassed() {
      return System.currentTimeMillis() - this.lastMS;
   }

   public boolean finished(long delay) {
      return System.currentTimeMillis() - this.lastMS >= delay;
   }

   public boolean every(long delay) {
      if (System.currentTimeMillis() - this.lastMS >= delay) {
         this.reset();
         return true;
      } else {
         return false;
      }
   }

    public long getElapsed() {
        return System.currentTimeMillis() - this.startTime;
    }
}
