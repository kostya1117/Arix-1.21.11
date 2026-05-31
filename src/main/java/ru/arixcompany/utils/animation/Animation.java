package ru.arixcompany.utils.animation;

import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.utils.math.TimerUtils;

public abstract class Animation {
   public TimerUtils timerUtil = new TimerUtils();
   @Setter
   @Getter
   protected int duration;
   @Setter
   @Getter
   protected double endPoint;
   @Getter
   protected Direction direction;

   public Animation(int ms, double endPoint) {
      this.duration = ms;
      this.endPoint = endPoint;
      this.direction = Direction.FORWARDS;
   }

   public Animation(int ms, double endPoint, Direction direction) {
      this.duration = ms;
      this.endPoint = endPoint;
      this.direction = direction;
   }

   public boolean finished(Direction direction) {
      return this.isDone() && this.direction.equals(direction);
   }

   public double getLinearOutput() {
      return 1.0 - (double)this.timerUtil.getTimePassed() / this.duration * this.endPoint;
   }

    public void reset() {
      this.timerUtil.reset();
   }

   public boolean isDone() {
      return this.timerUtil.hasReached(this.duration);
   }

   public void changeDirection() {
      this.setDirection(this.direction.opposite());
   }

    public void setDirection(Direction direction) {
      if (this.direction != direction) {
         this.direction = direction;
         this.timerUtil.setTime(System.currentTimeMillis() - (this.duration - Math.min((long)this.duration, this.timerUtil.getTimePassed())));
      }
   }

    protected boolean correctOutput() {
      return false;
   }

   public long getTimePassed() {
      return this.timerUtil.getTimePassed();
   }

   public float getOutput() {
      if (this.direction == Direction.FORWARDS) {
         return this.isDone() ? (float)this.endPoint : (float)(this.getEquation(this.timerUtil.getTimePassed()) * this.endPoint);
      } else if (this.isDone()) {
         return 0.0F;
      } else if (this.correctOutput()) {
         double revTime = Math.min((long)this.duration, Math.max(0L, this.duration - this.timerUtil.getTimePassed()));
         return (float)(this.getEquation(revTime) * this.endPoint);
      } else {
         return (float)((1.0 - this.getEquation(this.timerUtil.getTimePassed())) * this.endPoint);
      }
   }

   protected abstract double getEquation(double var1);
}
