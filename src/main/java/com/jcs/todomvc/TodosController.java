package com.jcs.todomvc;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/todos")
public class TodosController {

    public interface WireMethods {

        HttpEntity<Collection<ResourceWithUrl>> listAll();

        HttpEntity<ResourceWithUrl> getTodo(@PathVariable("todo-id") long id);

        HttpEntity<ResourceWithUrl> saveTodo(@RequestBody Todo todo);

        void deleteAllTodos();

        void deleteOneTodo(@PathVariable("todo-id") long id);

        HttpEntity<ResourceWithUrl> updateTodo(@PathVariable("todo-id") long id, @RequestBody Todo newTodo) ;

    }

    public static WireMethods WIRE_METHODS = new WireMethods() {

        private Set<Todo> todos = new HashSet<>();

        @Override
        public HttpEntity<Collection<ResourceWithUrl>> listAll() {
            List<ResourceWithUrl> resourceWithUrls = todos.stream().map(todo -> toResource(todo)).collect(Collectors.toList());
            return new ResponseEntity<>(resourceWithUrls, OK);
        }

        @Override
        public HttpEntity<ResourceWithUrl> getTodo(@PathVariable("todo-id") long id) {
            Optional<Todo> todoOptional = tryToFindById(id);

            if (!todoOptional.isPresent())
                return new ResponseEntity<>(NOT_FOUND);

            return respondWithResource(todoOptional.get(), OK);

        }

        @Override
        public HttpEntity<ResourceWithUrl> saveTodo(@RequestBody Todo todo) {
            todo.setId(todos.size() + 1);
            todos.add(todo);

            return respondWithResource(todo, CREATED);
        }

        @Override
        public void deleteAllTodos() {
            todos.clear();
        }

        @Override
        public void deleteOneTodo(@PathVariable("todo-id") long id) {
            Optional<Todo> todoOptional = tryToFindById(id);

            if ( todoOptional.isPresent() ) {
                todos.remove(todoOptional.get());
            }

        }

        @Override
        public HttpEntity<ResourceWithUrl> updateTodo(@PathVariable("todo-id") long id, @RequestBody Todo newTodo) {
            Optional<Todo> todoOptional = tryToFindById(id);

            if ( !todoOptional.isPresent() ) {
                return new ResponseEntity<>(NOT_FOUND);
            } else if ( newTodo == null ) {
                return new ResponseEntity<>(BAD_REQUEST);
            }

            todos.remove(todoOptional.get());

            Todo mergedTodo = todoOptional.get().merge(newTodo);
            todos.add(mergedTodo);

            return respondWithResource(mergedTodo, OK);
        }
        private Optional<Todo> tryToFindById(long id) {
            return todos.stream().filter(todo -> todo.getId() == id).findFirst();
        }

    };


    @RequestMapping(method = GET)
    public HttpEntity<Collection<ResourceWithUrl>> listAll() {
        return WIRE_METHODS.listAll();
    }

    @RequestMapping(value = "/{todo-id}", method = GET)
    public HttpEntity<ResourceWithUrl> getTodo(@PathVariable("todo-id") long id) {
        return WIRE_METHODS.getTodo(id);
    }

    @RequestMapping(method = POST,  headers = {"Content-type=application/json"})
    public HttpEntity<ResourceWithUrl> saveTodo(@RequestBody Todo todo) {
        return WIRE_METHODS.saveTodo(todo);
    }

    @RequestMapping(method = DELETE)
    public void deleteAllTodos() {
        WIRE_METHODS.deleteAllTodos();
    }

    @RequestMapping(value = "/{todo-id}", method = DELETE)
    public void deleteOneTodo(@PathVariable("todo-id") long id) {
        WIRE_METHODS.deleteOneTodo(id);
    }

    @RequestMapping(value = "/{todo-id}", method = PATCH, headers = {"Content-type=application/json"})
    public HttpEntity<ResourceWithUrl> updateTodo(@PathVariable("todo-id") long id, @RequestBody Todo newTodo) {
        return WIRE_METHODS.updateTodo(id, newTodo);
    }

    protected static String getHref(Todo todo) {
        return linkTo(methodOn(TodosController.class).getTodo(todo.getId())).withSelfRel().getHref();
    }

    protected static ResourceWithUrl toResource(Todo todo) {
        return new ResourceWithUrl(todo, getHref(todo));
    }

    protected static HttpEntity<ResourceWithUrl> respondWithResource(Todo todo, HttpStatus statusCode) {
        ResourceWithUrl resourceWithUrl = toResource(todo);

        return new ResponseEntity<>(resourceWithUrl, statusCode);
    }
}
