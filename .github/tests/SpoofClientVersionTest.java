import app.morphe.extension.twitter.patches.clientversion.SpoofClientVersionPatch;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Runs the production Java hooks without Android or an account. */
public final class SpoofClientVersionTest {
    private static final String VERSION = "12.19.1-release.0";
    private static final String USER_AGENT = "TwitterAndroid/12.19.1-release.0 (312190100-0) device";
    private static final String SPOOFED_USER_AGENT = "TwitterAndroid/12.41.0-prod.01 (312410001-0) device";

    private static void equal(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void request(String path, boolean chat) {
        SpoofClientVersionPatch.onRequest(URI.create("https://api.x.com" + path));
        equal(chat ? "12.41.0-prod.01" : VERSION, SpoofClientVersionPatch.legacyVersion(VERSION));
        equal(chat ? SPOOFED_USER_AGENT : USER_AGENT, SpoofClientVersionPatch.legacyUserAgent(USER_AGENT));
    }

    public static void main(String[] args) throws Exception {
        // The bytecode patch fills this operation list from the target APK.
        Field operations = SpoofClientVersionPatch.class.getDeclaredField("operations");
        operations.setAccessible(true);
        operations.set(null, Set.of("SendMessageCreateMutation", "GenerateXChatTokenMutation"));

        request("/1.1/xchat/session", true);
        request("/i/api/graphql/query/GenerateXChatTokenMutation", true);
        request("/i/api/graphql/query/SendMessageCreateMutation", true);
        request("/i/api/graphql/query/HomeTimeline", false);
        request("/1.1/account/settings.json", false);
        request("/1.1/SendMessageCreateMutation", false);

        // A chat request must not leak into the next request or another thread.
        request("/1.1/xchat/session", true);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                equal(VERSION, SpoofClientVersionPatch.legacyVersion(VERSION));
                request("/i/api/graphql/query/HomeTimeline", false);
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        other.start();
        other.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        equal("12.41.0-prod.01", SpoofClientVersionPatch.legacyVersion(VERSION));
        request("/i/api/graphql/query/HomeTimeline", false);
        SpoofClientVersionPatch.onRequest(null);
        equal(VERSION, SpoofClientVersionPatch.legacyVersion(VERSION));
        equal(USER_AGENT, SpoofClientVersionPatch.legacyUserAgent(USER_AGENT));
        equal(null, SpoofClientVersionPatch.userAgent(null));
        equal("unrelated", SpoofClientVersionPatch.userAgent("unrelated"));
        equal(SPOOFED_USER_AGENT, SpoofClientVersionPatch.userAgent(USER_AGENT));
        System.out.println("Chat version routing: all checks passed");
    }
}
