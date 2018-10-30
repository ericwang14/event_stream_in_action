package nile.events;


import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created ${name}
 *
 * @author wanggang
 * @since ${Date}
 */
public abstract class Event {
    public Subject subject;
    public String verb;
    public Context context;

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final DateTimeFormatter EVENT_DTF = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(DateTimeZone.UTC);

    public Event(String Shopper, String verb) {
        this.subject = new Subject(Shopper);
        this.verb = verb;
        this.context = new Context();
    }

    public static class Subject {
        public final String shopper;
        public Subject() {
            this.shopper = null;
        }
        public Subject(String shopper) {
            this.shopper = shopper;
        }
    }

    public static class Context {
        public final String timestamp;
        public Context() {
            this.timestamp = EVENT_DTF.print(new DateTime(DateTimeZone.UTC));
        }
    }
}
