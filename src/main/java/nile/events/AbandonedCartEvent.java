package nile.events;

import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created ${name}
 *
 * @author wanggang
 * @since ${Date}
 */
public class AbandonedCartEvent extends Event {
    private final DirectObject directObject;

    public AbandonedCartEvent(String Shopper, String cart) {
        super(Shopper, "abandon");
        this.directObject = new DirectObject(cart);
    }

    public static final class DirectObject {
        public final Cart cart;

        public DirectObject(String cart) {
            this.cart = new Cart(cart);
        }

        public static final class Cart {
            private static final int ABANDONED_AFTER_SECS = 1800;

            public List<Map<String, Object>> items =
                    new ArrayList<>();

            public Cart(String cart) {
                if (cart == null) {
                    return;
                }

                try {
                    this.items = MAPPER.readValue(cart, new TypeReference<Map<String, Object>>() {});
                } catch (IOException e) {
                    throw new RuntimeException("Problem parsing JSON cart", e);
                }
            }

            public void addItem(Map<String, Object> item) {
                this.items.add(item);
            }

            public String asJson() {
                try {
                    return MAPPER.writeValueAsString(this.items);
                } catch (Exception e) {
                    throw new RuntimeException("Problem writing JSON cart", e);
                }
            }

            public static boolean isAbandoned(String timestamp) {
                DateTime ts = EVENT_DTF.parseDateTime(timestamp);
                DateTime cutoff = new DateTime(DateTimeZone.UTC)
                        .minusSeconds(ABANDONED_AFTER_SECS);
                return ts.isBefore(cutoff);
            }
        }
    }
}
