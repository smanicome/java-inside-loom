import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Scheduler {
    private SchedulerExecutionPolicyImpl continuations;
    private interface SchedulerExecutionPolicyImpl {
        boolean isEmpty();
        void add(Continuation continuation);
        Continuation remove();
    }

    public enum SchedulerExecutionPolicy {
        STACK {
            private final ArrayDeque<Continuation> continuations = new ArrayDeque<>();
            @Override
            SchedulerExecutionPolicyImpl createImpl() {
                return new SchedulerExecutionPolicyImpl() {
                    @Override
                    public boolean isEmpty() {
                        return continuations.isEmpty();
                    }

                    @Override
                    public void add(Continuation continuation) {
                        continuations.offerLast(continuation);
                    }

                    @Override
                    public Continuation remove() {
                        return continuations.pollLast();
                    }
                };
            }
        },
        FIFO {
            private final ArrayDeque<Continuation> continuations = new ArrayDeque<>();
            @Override
            SchedulerExecutionPolicyImpl createImpl() {
                return new SchedulerExecutionPolicyImpl() {
                    @Override
                    public boolean isEmpty() {
                        return continuations.isEmpty();
                    }

                    @Override
                    public void add(Continuation continuation) {
                        continuations.offerLast(continuation);
                    }

                    @Override
                    public Continuation remove() {
                        return continuations.poll();
                    }
                };
            }
        },
        RANDOM {
            private final TreeMap<Integer, ArrayDeque<Continuation>> continuations = new TreeMap<>();
            @Override
            SchedulerExecutionPolicyImpl createImpl() {
                return new SchedulerExecutionPolicyImpl() {
                    @Override
                    public boolean isEmpty() {
                        return continuations.isEmpty();
                    }

                    @Override
                    public void add(Continuation continuation) {
                        var random = ThreadLocalRandom.current().nextInt();
                        continuations.computeIfAbsent(random, __ -> new ArrayDeque<>()).offer(continuation);
                    }

                    @Override
                    public Continuation remove() {
                        var random = ThreadLocalRandom.current().nextInt();
                        var key = continuations.floorKey(random);
                        if(key == null) {
                            key = continuations.firstKey();
                        }
                        var queue = continuations.get(key);
                        var continuation = queue.poll();
                        if(queue.isEmpty()) {
                            continuations.remove(key);
                        }
                        return continuation;
                    }
                };
            }
        };

        abstract SchedulerExecutionPolicyImpl createImpl();
    }

    Scheduler(SchedulerExecutionPolicy schedulerExecutionPolicy) {
        Objects.requireNonNull(schedulerExecutionPolicy);

        continuations = schedulerExecutionPolicy.createImpl();
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
            continuations.remove().run();
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
