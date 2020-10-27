import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
public class SchedulerTest {

    @Test
    public void stack(){
        var scope = new ContinuationScope("scope");
        var scheduler = new Scheduler(Scheduler.SchedulerExecutionPolicy.STACK);
        var str = new StringBuilder();
        var continuation1 = new Continuation(scope, () -> {
            str.append("start 1\n");
            scheduler.enqueue(scope);
            str.append("end 1\n");
        });
        var continuation2 = new Continuation(scope, () -> {
            str.append("start 2\n");
            scheduler.enqueue(scope);
            str.append("end 2\n");
        });
        var list = List.of(continuation1, continuation2);
        list.forEach(Continuation::run);
        scheduler.runLoop();
        assertEquals(str.toString(),"start 1\nstart 2\nend 2\nend 1\n");
    }
    @Test
    public void fifo(){
        var scope = new ContinuationScope("scope");
        var scheduler = new Scheduler(Scheduler.SchedulerExecutionPolicy.FIFO);
        var str = new StringBuilder();
        var continuation1 = new Continuation(scope, () -> {
            str.append("start 1\n");
            scheduler.enqueue(scope);
            str.append("end 1\n");
        });
        var continuation2 = new Continuation(scope, () -> {
            str.append("start 2\n");
            scheduler.enqueue(scope);
            str.append("end 2\n");
        });
        var list = List.of(continuation1, continuation2);
        list.forEach(Continuation::run);
        scheduler.runLoop();
        assertEquals(str.toString(),"start 1\nstart 2\nend 1\nend 2\n");
    }
}