package me.suhsaechan.suhlogger.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan("me.suhsaechan.suhlogger")
public class SuhLoggerAutoConfiguration { }