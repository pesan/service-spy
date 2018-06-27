package org.github.pesan.tools.servicespy.action;

import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActionServiceTest {

    private ActionService actionService;

    private @Mock RequestEntry requestEntry;
    private @Mock ResponseEntry responseEntry;
    private @Mock RequestIdGenerator requestIdGenerator;

    @Before
    public void setup() {
        this.actionService = new ActionService(requestIdGenerator, 3);
    }

    @Test
    public void shouldHaveNoActionsWhenNothingHasBeenLogged() {
        List<LogEntry> list = listActions();

        assertThat(list, equalTo(emptyList()));
    }

    @Test
    public void shouldAssignTheActionARequestIdWhenActionIsLogged() {
        when(requestIdGenerator.next()).thenReturn("28b21f-2aa281-2e12a");
        actionService.log(requestEntry, responseEntry);

        List<LogEntry> list = listActions();

        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getId(), equalTo("28b21f-2aa281-2e12a"));
        assertThat(list.get(0).getRequest(), equalTo(requestEntry));
        assertThat(list.get(0).getResponse(), equalTo(responseEntry));
    }

    @Test
    public void shouldSaveTheRequestAndResponseEntriesWhenActionIsLogged() {
        actionService.log(requestEntry, responseEntry);

        List<LogEntry> list = listActions();

        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getRequest(), equalTo(requestEntry));
        assertThat(list.get(0).getResponse(), equalTo(responseEntry));
    }

    @Test
    public void shouldSaveMultipleActionsWhenMoreThanOneIsLogged() {
        when(requestIdGenerator.next())
            .thenReturn("28b21f-2aa281-2e12a")
            .thenReturn("9bc93c-552cd9-2af11");

        actionService.log(requestEntry, responseEntry);
        actionService.log(requestEntry, responseEntry);

        List<LogEntry> list = listActions();

        assertThat(pluck(list, LogEntry::getId), equalTo(asList(
            "28b21f-2aa281-2e12a",
            "9bc93c-552cd9-2af11"
        )));
    }

    @Test
    public void shouldDropOldestActionsWhenThereAreMoreActionsThanTheLimit() {
        when(requestIdGenerator.next())
            .thenReturn("111111-111111-11111")
            .thenReturn("222222-222222-22222")
            .thenReturn("333333-333333-33333")
            .thenReturn("444444-444444-44444");

        actionService.log(requestEntry, responseEntry);
        actionService.log(requestEntry, responseEntry);
        actionService.log(requestEntry, responseEntry);
        actionService.log(requestEntry, responseEntry);

        List<LogEntry> list = listActions();

        assertThat(pluck(list, LogEntry::getId), equalTo(asList(
            "222222-222222-22222",
            "333333-333333-33333",
            "444444-444444-44444"
        )));
    }

    public void shouldRemoveAllActionWhenTheActionsAreCleared() {
        when(requestIdGenerator.next())
            .thenReturn("111111-111111-11111")
            .thenReturn("222222-222222-22222");

        actionService.log(requestEntry, responseEntry);
        actionService.log(requestEntry, responseEntry);

        actionService.clear();

        assertThat(listActions(), equalTo(Collections.emptyList()));
    }

    private static <U, V> List<V> pluck(List<U> entries, Function<U, V> mapper) {
        return entries.stream().map(mapper).collect(Collectors.toList());
    }

    private List<LogEntry> listActions() {
        return actionService.list().toList().blockingGet();
    }
}
