package com.demo.mota.app.event;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.event.Subscriber;
import com.demo.mota.engine.event.GameEvent;
import javafx.event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 把引擎命令产生的领域事件逐个发到 FXGL 的事件总线（同步、在调用线程上，源码：{@code EventBus.fireEvent}），
 * 并替每一局的订阅者保管订阅句柄。
 *
 * <p>FXGL 的事件总线是全局的，<b>不随 {@code startNewGame()} 重置</b>：每局注册的处理器必须在换局时注销，
 * 否则会一局一局累积。所以订阅一律经 {@link #subscribe}，由会话服务在 {@code onGameReset} 时 {@link #clear()}。
 */
public final class EngineEventBridge {

    private final List<Subscriber> subscriptions = new ArrayList<>();

    /** 按发生顺序逐个广播 */
    public void publish(List<GameEvent> events) {
        events.forEach(event -> FXGL.getEventBus().fireEvent(new MotaEvent(event)));
    }

    /** 订阅一类事件，直到这一局结束 */
    public void subscribe(EventType<MotaEvent> type, Consumer<GameEvent> handler) {
        subscriptions.add(FXGL.getEventBus().addEventHandler(type, event -> handler.accept(event.payload())));
    }

    /** 注销本局的全部订阅 */
    public void clear() {
        subscriptions.forEach(Subscriber::unsubscribe);
        subscriptions.clear();
    }
}
