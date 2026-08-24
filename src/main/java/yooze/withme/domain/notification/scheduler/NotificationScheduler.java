package yooze.withme.domain.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupRoundRepository;
import yooze.withme.domain.notification.enums.NotificationType;
import yooze.withme.domain.notification.service.NotificationCommandService;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.TodoRepository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final GroupRoundRepository groupRoundRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final NotificationCommandService notificationCommandService;

    /**
     * 모임 일정 D-1 리마인더 — 매일 오전 9시 실행.
     * 내일 일정인 모임을 조회해 활성 멤버 전체에게 알림을 발송한다.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendGroupReminderNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<GroupRound> rounds = groupRoundRepository.findByRoundDateWithGroup(tomorrow);

        log.info("[*] 모임 D-1 리마인더 - 대상 회차 수: {}", rounds.size());

        for (GroupRound round : rounds) {
            String groupName = round.getGroup().getName();
            String title = "모임 일정 알림";
            String body = String.format("내일 '%s' 모임이 있어요! (%s %s)",
                    groupName, round.getRoundDate(), round.getRoundTime());

            List<Long> userIds = groupMemberRepository.findActiveUserIdsByGroupId(round.getGroup().getId());
            List<User> users = userRepository.findAllById(userIds);

            for (User user : users) {
                notificationCommandService.send(user, NotificationType.GROUP_REMINDER, title, body);
            }
        }
    }

    /**
     * Todo 마감 알림 — 매일 오전 9시 실행.
     * 내일 마감이고 알림 설정된 미완료 todo의 담당자에게 알림을 발송한다.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendTodoDeadlineNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Todo> todos = todoRepository.findDueTodosForNotification(tomorrow);

        log.info("[*] Todo 마감 알림 - 대상 수: {}", todos.size());

        for (Todo todo : todos) {
            String title = "Todo 마감 알림";
            String body = String.format("내일 마감인 할 일이 있어요! '%s'", todo.getTitle());
            notificationCommandService.send(todo.getUser(), NotificationType.TODO_DEADLINE, title, body);
        }
    }
}
