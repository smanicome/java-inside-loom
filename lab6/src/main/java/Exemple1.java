import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Exemple1 {
    public static void main(String[] args) {
        final var lock = new ReentrantLock();

        var scope = new ContinuationScope("scope");
        var continuation1 = new Continuation(scope, () -> {
            System.out.println("start 1");
            Continuation.yield(scope);
            System.out.println("middle 1");
            Continuation.yield(scope);
            System.out.println("end 1");
        });
        var continuation2 = new Continuation(scope, () -> {
            System.out.println("start 2");
            Continuation.yield(scope);
            System.out.println("middle 2");
            Continuation.yield(scope);
            System.out.println("end 2");

        });
        var list = List.of(continuation1, continuation2);

        while(!list.stream().allMatch(Continuation::isDone)) {
            list.stream()
                .filter(c -> !c.isDone())
                .forEach(Continuation::run);
        }
    }
}
