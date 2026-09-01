package com.booksharing;

import org.springframework.boot.SpringApplication;

public class TestBookSharingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(BookSharingBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
