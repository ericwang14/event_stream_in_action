package nile.task;

import nile.events.AbandonedCartEvent;
import org.apache.samza.config.Config;
import org.apache.samza.storage.kv.Entry;
import org.apache.samza.storage.kv.KeyValueIterator;
import org.apache.samza.storage.kv.KeyValueStore;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.*;

import java.util.Map;

/**
 * Created ${name}
 *
 * @author wanggang
 * @since ${Date}
 */
public class AbandonedCartsStreamTask implements StreamTask, InitableTask, WindowableTask {

    private KeyValueStore<String, String> store;

    @SuppressWarnings("unchecked")
    @Override
    public void init(Config config, TaskContext context) {
        this.store = (KeyValueStore<String, String>)
                context.getStore("nile-carts");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(IncomingMessageEnvelope envelope, MessageCollector collector,
                        TaskCoordinator coordinator) {
        Map<String, Object> event =
                (Map<String, Object>) envelope.getMessage();
        String verb = (String) event.get("verb");
        String shopper = (String) ((Map<String, Object>)
                event.get("subject")).get("shopper");

        if (verb.equals("add")) {
            String timestamp = (String) ((Map<String, Object>)
                    event.get("context")).get("timestamp");
            Map<String, Object> item = (Map<String, Object>)
                    ((Map<String, Object>) event.get("directObject")).get("item");
            AbandonedCartEvent.DirectObject.Cart cart = new AbandonedCartEvent.DirectObject.Cart(store.get(asCartKey(shopper)));
            cart.addItem(item);
            store.put(asTimestampKey(shopper), timestamp);
            store.put(asCartKey(shopper), cart.asJson());
        } else if (verb.equals("place")) {
            resetShopper(shopper);
        }
    }

    @Override
    public void window(MessageCollector collector, TaskCoordinator coordinator) {
        KeyValueIterator<String, String> entries = store.all();

        while (entries.hasNext()) {
            Entry<String, String> entry = entries.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (isTimestampKey(key) && AbandonedCartEvent.DirectObject.Cart.isAbandoned(value)) {
                String shopper = extractShopper(key);
                String cart = store.get(asCartKey(shopper));
                AbandonedCartEvent event =
                        new AbandonedCartEvent(shopper, cart);
                collector.send(new OutgoingMessageEnvelope(
                        new SystemStream("kafka", "derived-events-ch04"), event));
                resetShopper(shopper);
            }
        }
    }

    private static String asTimestampKey(String shopper) {
        return shopper + "-ts";
    }
    private static boolean isTimestampKey(String key) {
        return key.endsWith("-ts");
    }
    private static String extractShopper(String key) {
        return key.substring(0, key.lastIndexOf('-'));
    }
    private static String asCartKey(String shopper) {
        return shopper + "-cart";
    }
    private void resetShopper(String shopper) {
        store.delete(asTimestampKey(shopper));
        store.delete(asCartKey(shopper));
    }
}
