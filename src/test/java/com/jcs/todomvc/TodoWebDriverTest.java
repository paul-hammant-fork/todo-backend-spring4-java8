package com.jcs.todomvc;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.seleniumhq.selenium.fluent.FluentWebDriver;
import org.seleniumhq.selenium.fluent.FluentWebElement;
import org.seleniumhq.selenium.fluent.TestableString;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.jcs.todomvc.TodosController.respondWithResource;
import static com.jcs.todomvc.TodosController.toResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;
import static org.seleniumhq.selenium.fluent.Period.secs;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "server.port=9000",
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TodoWebDriverTest {

  public static class NoopWireMethods implements TodosController.WireMethods {

    public HttpEntity<Collection<ResourceWithUrl>> listAll() {
      throw new UnsupportedOperationException("not expected");
    }

    public HttpEntity<ResourceWithUrl> getTodo(@PathVariable("todo-id") long id) {
      throw new UnsupportedOperationException("not expected");
    }

    public HttpEntity<ResourceWithUrl> saveTodo(@RequestBody Todo todo) {
      throw new UnsupportedOperationException("not expected");
    }

    public void deleteAllTodos() {
      throw new UnsupportedOperationException("not expected");
    }

    public void deleteOneTodo(@PathVariable("todo-id") long id) {
      throw new UnsupportedOperationException("not expected");
    }

    public HttpEntity<ResourceWithUrl> updateTodo(@PathVariable("todo-id") long id, @RequestBody Todo newTodo) {
      throw new UnsupportedOperationException("not expected");
    }

  }

  private static ChromeDriver DRIVER;
  private static FluentWebDriver FWD;
  private static int testNum;

  private static Todo todo;
  private static long id;

  @BeforeClass
  public static void sharedForAllTests() {
    // Keep the WebDriver browser window open between tests
    DRIVER = new ChromeDriver();
    FWD = new FluentWebDriver(DRIVER);
  }

  @AfterClass
  public static void tearDown() {
    DRIVER.close();
    DRIVER.quit();
  }

  private String domain;

  @Before
  public void perTest() {
    // anySubDomainOf.devd.io maps to 127.0.0.1
    // I sure hope those people don't let the domain go, or remap it
    // it is a decent way to ensure nothing is shared between tests (mostly)
    domain = "http://t" + testNum++ + ".devd.io:9000";
    todo = null;
    id = -1;
  }

  @Test
  public void initialListShouldBeASingleSetupEntry() throws InterruptedException {

    initialListShouldBeASingleSetupEntry_mockSetup();
    openTodoPage();
    listInPageShouldBe("Win Lottery|Climb Everest");
  }

  @Test
  public void addItemToList() throws InterruptedException {
    addItemToList_mockSetup();

    openTodoPage();
    FWD.input(id("new-todo")).sendKeys("Buy eggs - check they're not cracked" + Keys.RETURN);
    listInPageShouldBe("One|Two|Buy eggs - check they're not cracked");
    waitForTodoToBeUpdatedAsync();
    assertThat(todo.getTitle(), equalTo("Buy eggs - check they're not cracked"));
  }

  private void waitForTodoToBeUpdatedAsync() throws InterruptedException {
    long time = 0;
    while (todo == null && id == -1 && time < 1000) {
      Thread.sleep(10);
      time += 10;
    }
  }

  private void openTodoPage() {
    DRIVER.get(domain + "/index.html?" + domain + "/todos");
  }

  @Test
  public void deleteAnItem() throws InterruptedException {

    deleteAnItem_mockSetup();

    openTodoPage();

    listInPageShouldBe("Sleep");
    FluentWebElement sleepRow = FWD.ul(id("todo-list"));
    clickOnRowToActivateDeleteButton(sleepRow);
    sleepRow.button(className("destroy")).click();
    listInPageShouldBe("");
    assertThat(id, equalTo(1L));
  }

  private static void listInPageShouldBe(String shouldBe) {
    FWD.ul(id("todo-list")).getText((TestableString.StringChanger) s -> s.replace("\n", "|"))
            .within(secs(1)).shouldBe(shouldBe);
  }

  private static void clickOnRowToActivateDeleteButton(FluentWebElement row) {
    new Actions(DRIVER).moveToElement(row.getWebElement()).click().perform();
  }

  // Mocks - static setup methods

  public static void initialListShouldBeASingleSetupEntry_mockSetup() {
    TodosController.WIRE_METHODS = new NoopWireMethods() {

      @Override
      public HttpEntity<Collection<ResourceWithUrl>> listAll() {
        return new ResponseEntity<>(new HashSet<Todo>() {{
          add(new Todo(1, "Win Lottery", false, 1));
          add(new Todo(2, "Climb Everest", false, 2));
        }}.stream().map(todo -> toResource(todo)).collect(Collectors.toList()), OK);

      }
    };
  }

  public static void addItemToList_mockSetup() {
    TodosController.WIRE_METHODS = new NoopWireMethods() {
      @Override
      public HttpEntity<Collection<ResourceWithUrl>> listAll() {
        return new ResponseEntity<>(new HashSet<Todo>() {{
          add(new Todo(1, "One", false, 1));
          add(new Todo(2, "Two", false, 2));
        }}.stream().map(todo -> toResource(todo)).collect(Collectors.toList()), OK);
      }

      @Override
      public HttpEntity<ResourceWithUrl> saveTodo(@RequestBody Todo t) {
        todo = t;
        return respondWithResource(todo, CREATED);
      }

    };
  }


  public static void deleteAnItem_mockSetup() {
    TodosController.WIRE_METHODS = new NoopWireMethods() {
      @Override
      public HttpEntity<Collection<ResourceWithUrl>> listAll() {

        return new ResponseEntity<>(new HashSet<Todo>() {{
          add(new Todo(1, "Sleep", false, 1));
        }}.stream().map(todo -> toResource(todo)).collect(Collectors.toList()), OK);

      }

      @Override
      public void deleteOneTodo(@PathVariable("todo-id") long i) {
        id = i;
      }

    };
  }


}
