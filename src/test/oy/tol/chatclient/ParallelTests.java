package oy.tol.chatclient;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

public class ParallelTests {

    @Execution(ExecutionMode.CONCURRENT)
    public void executeChatPosts() {

    }

    @Execution(ExecutionMode.CONCURRENT)
    @TestFactory
    Collection<DynamicTest> test_parallel_dynamictests1() {
        return Arrays.asList(dynamicTest("1st dynamic test", () -> {
            System.out.println(Thread.currentThread().getName() + " => 1st dynamic test");
        }), dynamicTest("2nd dynamic test", () -> {
            System.out.println(Thread.currentThread().getName() + " => 2nd dynamic test");
        }), dynamicTest("3rd dynamic test", () -> {
            System.out.println(Thread.currentThread().getName() + " => 3rd dynamic test");
        }));
    }
}
