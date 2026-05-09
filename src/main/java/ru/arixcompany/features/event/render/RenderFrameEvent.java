package ru.arixcompany.features.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.Event;

@Setter
@Getter
@AllArgsConstructor
public class RenderFrameEvent extends Event {

    private float partialTicks;

    public static class Pre extends RenderFrameEvent {
        public Pre(float partialTicks) {
            super(partialTicks);
        }
    }

    public static class Post extends RenderFrameEvent {
        public Post(float partialTicks) {
            super(partialTicks);
        }
    }

}
