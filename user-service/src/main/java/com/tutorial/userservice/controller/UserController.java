package com.tutorial.userservice.controller;

import com.tutorial.userservice.entity.User;
import com.tutorial.userservice.model.Bike;
import com.tutorial.userservice.model.Car;
import com.tutorial.userservice.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.BaggageInScope;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanName;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    Tracer tracer;

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        List<User> users = userService.getAll();
        if(users.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @NewSpan("GetUserById")
    public ResponseEntity<User> getById(@PathVariable("id") @SpanTag("TagTest")  int id) {
        log.info("Getting User");
        try (Tracer.SpanInScope ws = this.tracer.withSpan(this.tracer.currentSpan())) {
            BaggageInScope businessProcess = this.tracer.createBaggage("BUSINESS_PROCESS").set("ALM");
            businessProcess.set("ALM2");
            log.info("Getting User2");
        }
        log.info("Getting User3");
        User user = userService.getUserById(id);
        if(user == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    @PostMapping()
    public ResponseEntity<User> save(@RequestBody User user) {
        User userNew = userService.save(user);
        return ResponseEntity.ok(userNew);
    }

    @CircuitBreaker(name = "carsCB", fallbackMethod = "fallbackGetCars")
    @GetMapping("/cars/{userId}")
    public ResponseEntity<List<Car>> getCars(@PathVariable("userId") int userId) {
        log.info("Getting Cars");
        try (Tracer.SpanInScope ws = this.tracer.withSpan(this.tracer.currentSpan())) {
            BaggageInScope businessProcess = this.tracer.createBaggage("BUSINESS_PROCESS").set("ALM");
            businessProcess.set("ALM2");

            log.info("Getting Cars2 tracer.getAllBaggage()");
        }
        log.info("Getting Cars3");

        User user = userService.getUserById(userId);
        if(user == null)
            return ResponseEntity.notFound().build();
        List<Car> cars = userService.getCars(userId);
        return ResponseEntity.ok(cars);
    }

    @CircuitBreaker(name = "carsCB", fallbackMethod = "fallbackSaveCar")
    @PostMapping("/saveCar")
    public ResponseEntity<Car> saveCar(@RequestBody Car car) {
        if(userService.getUserById(Optional.ofNullable(car).map(Car::getUserId).orElse(null)) == null) {
            return ResponseEntity.notFound().build();
        }
        Car carNew = userService.saveCar(car.getUserId(), car);
        return ResponseEntity.ok(carNew);
    }

    @CircuitBreaker(name = "bikesCB", fallbackMethod = "fallbackGetBikes")
    @GetMapping("/bikes/{userId}")
    public ResponseEntity<List<Bike>> getBikes(@PathVariable("userId") int userId) {
        User user = userService.getUserById(userId);
        if(user == null)
            return ResponseEntity.notFound().build();
        List<Bike> bikes = userService.getBikes(userId);
        return ResponseEntity.ok(bikes);
    }

    @CircuitBreaker(name = "bikesCB", fallbackMethod = "fallbackSaveBike")
    @PostMapping("/saveBike")
    public ResponseEntity<Bike> saveBike(@RequestBody Bike bike) {
        if(userService.getUserById(Optional.ofNullable(bike).map(Bike::getUserId).orElse(null)) == null) {
            return ResponseEntity.notFound().build();
        }
        Bike bikeNew = userService.saveBike(bike.getUserId(), bike);
        return ResponseEntity.ok(bikeNew);
    }

    @CircuitBreaker(name = "allCB", fallbackMethod = "fallbackGetAll")
    @GetMapping("/getAll/{userId}")
    public ResponseEntity<Map<String, Object>> getAllVehicles(@PathVariable("userId") int userId) {
        Map<String, Object> result = userService.getUserAndVehicles(userId);
        return ResponseEntity.ok(result);
    }

    // Fallbacks
    private ResponseEntity<Car> fallbackGetCars(@PathVariable("userId") int userId, RuntimeException e) {
        return new ResponseEntity(String.format("User %s has all their cars at body shop", userId), HttpStatus.OK);
    }

    private ResponseEntity<Car> fallbackSaveCar(@RequestBody Car car, RuntimeException e) {
        return new ResponseEntity(String.format("User %s does not have money enough to buy this car", car.getUserId()), HttpStatus.OK);
    }

    private ResponseEntity<List<Bike>> fallbackGetBikes(@PathVariable("userId") int userId, RuntimeException e) {
        return new ResponseEntity(String.format("User %s has all their bikes at body shop", userId), HttpStatus.OK);
    }

    private ResponseEntity<Bike> fallbackSaveBike(@RequestBody Bike bike, RuntimeException e) {
        return new ResponseEntity(String.format("User %s does not have money enough to buy this bike", bike.getUserId()), HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> fallbackGetAll(@PathVariable("userId") int userId, RuntimeException e) {
        return new ResponseEntity(String.format("User %s has all their vehicles at body shop", userId), HttpStatus.OK);
    }
}
