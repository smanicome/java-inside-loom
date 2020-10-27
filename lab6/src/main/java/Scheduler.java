import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class Scheduler {
    private final ArrayList<Continuation> continuations = new ArrayList<>();
    private final Runnable execution;
    public enum SchedulerExecutionPolicy {
        STACK, FIFO, RANDOM
    }

    Scheduler(SchedulerExecutionPolicy schedulerExecutionPolicy) {
        Objects.requireNonNull(schedulerExecutionPolicy);

        execution = switch (schedulerExecutionPolicy) {
            case FIFO -> this::fifoOperation;
            case STACK -> this::stackOperation;
            case RANDOM -> this::randomOperation;
        };
    }

    private void stackOperation() {
        var c = continuations.remove(continuations.size() - 1);
        if(!c.isDone()) {
            c.run();
        }
    }

    private void fifoOperation() {
        var c = continuations.remove(0);
        if(!c.isDone()) {
            c.run();
        }
    }
    private void randomOperation() {
        var c = continuations.remove(ThreadLocalRandom.current().nextInt(0, continuations.size()));
        if(!c.isDone()) {
            c.run();
        }
    }

    public void enqueue(ContinuationScope scope) {
        Objects.requireNonNull(scope);
        var c = Continuation.getCurrentContinuation(scope);

        Objects.requireNonNull(c, "No continuations");

        continuations.add(c);
        Continuation.yield(scope);
    }

    public void runLoop() {
        if (continuations.isEmpty()) {
            throw new IllegalStateException();
        }

        while(!continuations.isEmpty()) {
            execution.run();
        }
    }

    public static void main(String[] args) {
        var scope = new ContinuationScope("scope");
        var scheduler = new Scheduler(SchedulerExecutionPolicy.RANDOM);
        var continuation1 = new Continuation(scope, () -> {
            System.out.println("start 1");
            scheduler.enqueue(scope);
            System.out.println("middle 1");
            scheduler.enqueue(scope);
            System.out.println("end 1");
        });
        var continuation2 = new Continuation(scope, () -> {
            System.out.println("start 2");
            scheduler.enqueue(scope);
            System.out.println("middle 2");
            scheduler.enqueue(scope);
            System.out.println("end 2");
        });
        var list = List.of(continuation1, continuation2);
        list.forEach(Continuation::run);
        scheduler.runLoop();
    }
}
