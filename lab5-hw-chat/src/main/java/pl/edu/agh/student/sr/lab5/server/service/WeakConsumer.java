package pl.edu.agh.student.sr.lab5.server.service;

import pl.edu.agh.student.sr.lab5.proto.Chat;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class WeakConsumer<T> implements Consumer<T> {

    private WeakReference<Consumer<T>> consumer = null;
    private Consumer<WeakConsumer<T>> onDead = null;

    public WeakConsumer(Consumer<T> consumer) {
        this(new WeakReference<>(consumer), null);
    }

    public WeakConsumer(Consumer<T> consumer, Consumer<WeakConsumer<T>> onDead) {
        this(new WeakReference<>(consumer), onDead);
    }

    public WeakConsumer(WeakReference<Consumer<T>> consumer) {
        this(consumer, null);
    }

    public WeakConsumer(WeakReference<Consumer<T>> consumer, Consumer<WeakConsumer<T>> onDead) {
        this.consumer = consumer;
        this.onDead = onDead;
    }

    @Override
    public void accept(T t) {
        Consumer<T> consumer;
        if (this.consumer != null && (consumer = this.consumer.get()) != null) {
            consumer.accept(t);
        } else {
            if (onDead != null) {
                onDead.accept(this);
            }
        }
    }

    public void setConsumer(WeakReference<Consumer<T>> consumer) {
        this.consumer = consumer;
    }

    public void setOnDead(Consumer<WeakConsumer<T>> onDead) {
        this.onDead = onDead;
    }

}
