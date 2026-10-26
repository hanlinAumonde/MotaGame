package com.demo.mota.app;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.SubScene;
import com.demo.mota.app.input.GameplayInput;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.scene.equipment.EquipmentSubScene;
import com.demo.mota.scene.equipment.EquipmentView;
import com.demo.mota.scene.equipset.EquipmentSetSubScene;
import com.demo.mota.scene.equipset.EquipmentSetView;
import com.demo.mota.scene.game.GameScreen;
import com.demo.mota.scene.gamemenu.GameMenuScene;
import com.demo.mota.scene.gamemenu.GameMenuView;
import com.demo.mota.scene.gamemenu.option.GameOption;
import com.demo.mota.scene.gameover.GameOverSubScene;
import com.demo.mota.scene.inventory.InventorySubScene;
import com.demo.mota.scene.inventory.InventoryView;
import com.demo.mota.scene.saveload.SaveLoadMode;
import com.demo.mota.scene.saveload.SaveLoadSubScene;
import com.demo.mota.scene.saveload.SaveLoadView;
import com.demo.mota.scene.skill.SkillSetupSubScene;
import com.demo.mota.scene.skill.SkillSetupView;
import com.demo.mota.scene.title.TitleMenu;

import java.util.Objects;

/**
 * 界面的组装处（取代原来的 {@code MotaController}）：建好对局画面与各子场景，把它们之间的跳转接起来。
 *
 * <p>分两步装配，对应 FXGL 的两个时机：
 * <ol>
 *   <li>本对象在 {@code initSettings} 里创建、交给 {@link MotaSceneFactory}——标题菜单与游戏菜单在服务初始化阶段、
 *       内容载入之前就由 FXGL 构造，它们拿到的是本对象（{@link TitleMenu.Actions} / {@link GameMenuScene.Host}），
 *       进入时才经它取内容与会话；</li>
 *   <li>{@link #init()} 在 {@code initInput} 时调用（FX 线程，全部服务已就绪），建对局画面与其余子场景。</li>
 * </ol>
 * 子场景各建一个、反复叠上：FXGL 每次 push 都会调它们的 {@code onCreate}，各自在那里按当前这一局重建状态。
 */
public final class MotaScenes implements TitleMenu.Actions, GameMenuScene.Host, GameScreen.Menus {

    private SessionService sessions;
    private Navigator navigator;
    private GameScreen game;
    private GameplayInput gameplayInput;
    private GameMenuView gameMenuView;

    private SaveLoadSubScene saveLoad;
    private GameOverSubScene gameOver;
    private InventorySubScene inventory;
    private EquipmentSubScene equipment;
    private EquipmentSetSubScene equipmentSets;
    private SkillSetupSubScene skillSetup;

    /** 服务就绪之后（{@code initInput}）调用一次 */
    public void init() {
        sessions = FXGL.getService(SessionService.class);
        AssetService assets = FXGL.getService(AssetService.class);

        navigator = new Navigator(this::resetHeld);
        game = new GameScreen(sessions, assets, navigator, this);
        gameplayInput = new GameplayInput(game, sessions.content().rules());
        Runnable returnToGame = this::returnToGame;

        gameMenuView = new GameMenuView(assets);
        saveLoad = new SaveLoadSubScene(navigator, sessions, new SaveLoadView());
        gameOver = new GameOverSubScene(navigator, sessions, saveLoad, game::undoLastAction);
        inventory = new InventorySubScene(navigator, returnToGame, sessions, new InventoryView(assets));
        equipment = new EquipmentSubScene(navigator, returnToGame, sessions, new EquipmentView(assets));
        equipmentSets = new EquipmentSetSubScene(navigator, returnToGame, sessions,
                new EquipmentSetView(assets), game::showMessage);
        skillSetup = new SkillSetupSubScene(navigator, returnToGame, sessions, new SkillSetupView(assets));
    }

    public GameScreen game() {
        return Objects.requireNonNull(game, "界面尚未装配");
    }

    public GameplayInput gameplayInput() {
        return Objects.requireNonNull(gameplayInput, "界面尚未装配");
    }

    /**
     * 清掉对局里「按住」的状态（修饰键、方向键）：离开对局之前、窗口失焦时、回到对局时。
     * 离开 / 失焦时必须先于 FXGL 的 {@code clearAll()}，见 {@link GameplayInput} 的「陷阱」。
     */
    public void resetHeld() {
        gameplayInput.resetChords();
        game.resetHeld();
    }

    // ==================== 标题菜单 ====================

    @Override
    public boolean hasAnySave() {
        return sessions.saves().hasAnySave();
    }

    @Override
    public void startNewGame() {
        sessions.request(sessions.prepareNewGame(), "");
        FXGL.getGameController().startNewGame();
    }

    @Override
    public void openLoad() {
        saveLoad.open(SaveLoadMode.LOAD, false);
    }

    @Override
    public void exit() {
        FXGL.getDialogService().showConfirmationBox("退出游戏？", yes -> {
            if (yes) {
                FXGL.getGameController().exit();
            }
        });
    }

    // ==================== 游戏菜单 ====================

    @Override
    public GameSession session() {
        return sessions.session();
    }

    @Override
    public GameMenuView view() {
        return gameMenuView;
    }

    @Override
    public void openOption(GameOption option) {
        navigator.replaceTop(switch (option) {
            case ITEMS -> inventory;
            case EQUIPMENT -> equipment;
            case SKILLS -> skillSetup;
            case EQUIPMENT_SETS -> equipmentSets;
        });
    }

    @Override
    public void openSaveLoad(SaveLoadMode mode) {
        saveLoad.open(mode, true);
    }

    /** 从叠在上面的界面回到对局（游戏菜单与各对局内界面离开时调用） */
    @Override
    public void returnToGame() {
        gameplayInput.resetChords();
        game.returnToGame();
    }

    // ==================== 对局中叠上的界面 ====================

    @Override
    public SubScene equipment() {
        return equipment;
    }

    @Override
    public SubScene equipmentSets() {
        return equipmentSets;
    }

    @Override
    public SubScene skillSetup() {
        return skillSetup;
    }

    @Override
    public SubScene gameOver() {
        return gameOver;
    }
}
