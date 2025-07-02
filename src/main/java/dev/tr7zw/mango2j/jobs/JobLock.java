package dev.tr7zw.mango2j.jobs;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import lombok.Getter;

@ApplicationScope
@Component
public class JobLock {

    @Getter
    private final ReentrantLock lock = new ReentrantLock();
    
}
