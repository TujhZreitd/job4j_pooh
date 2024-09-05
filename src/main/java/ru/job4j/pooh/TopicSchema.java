package ru.job4j.pooh;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class TopicSchema implements Schema {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Receiver>> receivers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<String>> data = new ConcurrentHashMap<>();
    private final Condition condition = new Condition();

    @Override
    public void addReceiver(Receiver receiver) {
        receivers.putIfAbsent(receiver.name(), new CopyOnWriteArrayList<>());
        receivers.get(receiver.name()).add(receiver);
        condition.on();
    }

    @Override
    public void publish(Message message) {
        data.putIfAbsent(message.name(), new LinkedBlockingQueue<>());
        data.get(message.name()).add(message.text());
        condition.on();
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            for (String queueKey : receivers.keySet()) {
                BlockingQueue<String> queue = data.getOrDefault(queueKey, new LinkedBlockingQueue<>());
                CopyOnWriteArrayList<Receiver> receiverByQueue = receivers.get(queueKey);
                Iterator<String> itMessages = queue.iterator();
                while (itMessages.hasNext()) {
                    String data = itMessages.next();
                    for (Receiver receiver : receiverByQueue) {
                        receiver.receive(data);
                    }
                    queue.remove();
                }
            }
            condition.off();
            try {
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
