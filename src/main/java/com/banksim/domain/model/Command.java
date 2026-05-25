package com.banksim.domain.model;

/**
 * Command pattern: banking operation object that can be executed and rolled back.
 */
public interface Command {
    void execute();

    void rollback();
}
