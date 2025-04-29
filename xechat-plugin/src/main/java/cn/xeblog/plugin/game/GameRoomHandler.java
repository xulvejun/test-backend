package cn.xeblog.plugin.game;

import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.game.*;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.commons.enums.Game;
import cn.xeblog.commons.enums.InviteStatus;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.action.MessageAction;
import cn.xeblog.plugin.cache.DataCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author anlingyi
 * @date 2022/5/25 10:18 上午
 */
public abstract class GameRoomHandler implements GameRoomEventHandler {

    /**
     * 邀请超时任务缓存
     */
    private Map<String, Timer> timeoutTask;

    /**
     * 当前游戏房间
     */
    protected GameRoom gameRoom;

    /**
     * 当前是否为房主
     */
    protected boolean isHomeowner;

    /**
     * 已开始游戏玩家计数
     */
    private AtomicInteger playerGameStartedCounter;

    /**
     * 创建游戏房间
     *
     * @param game     游戏
     * @param nums     房间人数
     * @param gameMode 游戏模式
     */
    public void createRoom(Game game, int nums, String gameMode) {
        MessageAction.send(new CreateGameRoomDTO(game, nums, gameMode), Action.CREATE_GAME_ROOM);
    }

    /**
     * 邀请玩家
     *
     * @param username 玩家昵称
     */
    public void invitePlayer(String username) {
        if (gameRoom == null) {
            ConsoleAction.showSimpleMsg("请先创建游戏房间！");
            return;
        }

        User player = DataCache.getUser(username);
        if (player == null) {
            ConsoleAction.showSimpleMsg("该用户不存在！");
            return;
        }

        GameRoomMsgDTO msg = new GameRoomMsgDTO();
        msg.setRoomId(gameRoom.getId());
        msg.setMsgType(GameRoomMsgDTO.MsgType.PLAYER_INVITE);
        msg.setContent(new GameInviteDTO(player.getId()));
        MessageAction.send(msg, Action.GAME_ROOM);

        Timer timer = new Timer();
        timeoutTask.put(player.getId(), timer);
        timer.schedule(new TimerTask() {
            int time = 30;

            @Override
            public void run() {
                boolean timeout = --time < 0;
                if (gameRoom == null || timeout) {
                    timer.cancel();
                }

                if (gameRoom == null) {
                    return;
                }

                if (timeout) {
                    GameRoomMsgDTO msg = new GameRoomMsgDTO();
                    msg.setRoomId(gameRoom.getId());
                    msg.setMsgType(GameRoomMsgDTO.MsgType.PLAYER_INVITE_RESULT);
                    msg.setContent(new GameInviteResultDTO(InviteStatus.TIMEOUT, null, player.getId()));
                    MessageAction.send(msg, Action.GAME_ROOM);
                }
            }
        }, 0, 1000);
    }

    /**
     * 游戏开始请求
     */
    public void gameStart() {
        GameRoomMsgDTO msg = new GameRoomMsgDTO();
        msg.setRoomId(gameRoom.getId());
        msg.setMsgType(GameRoomMsgDTO.MsgType.GAME_START);
        MessageAction.send(msg, Action.GAME_ROOM);
    }

    /**
     * 游戏结束请求
     */
    public void gameOver() {
        GameRoomMsgDTO msg = new GameRoomMsgDTO();
        msg.setRoomId(gameRoom.getId());
        msg.setMsgType(GameRoomMsgDTO.MsgType.GAME_OVER);
        MessageAction.send(msg, Action.GAME_ROOM);
    }

    /**
     * 关闭房间请求
     */
    public void closeRoom() {
        GameRoomMsgDTO msg = new GameRoomMsgDTO();
        msg.setRoomId(gameRoom.getId());
        msg.setMsgType(GameRoomMsgDTO.MsgType.ROOM_CLOSE);
        MessageAction.send(msg, Action.GAME_ROOM);
    }

    /**
     * 玩家准备请求
     */
    public void playerReady() {
        GameRoomMsgDTO msg = new GameRoomMsgDTO();
        msg.setRoomId(gameRoom.getId());
        msg.setMsgType(GameRoomMsgDTO.MsgType.PLAYER_READY);
        MessageAction.send(msg, Action.GAME_ROOM);
    }

    /**
     * 玩家游戏已开始请求
     */
    public void playerGameStarted() {
        GameRoomMsgDTO msg = new GameRoomMsgDTO();
        msg.setRoomId(gameRoom.getId());
        msg.setMsgType(GameRoomMsgDTO.MsgType.PLAYER_GAME_STARTED);
        MessageAction.send(msg, Action.GAME_ROOM);
    }

    @Override
    public void roomCreated(GameRoom gameRoom) {
        this.timeoutTask = new HashMap<>();
        this.isHomeowner = true;
        this.playerGameStartedCounter = new AtomicInteger(gameRoom.getNums());
        roomOpened(gameRoom);
    }

    @Override
    public void playerJoined(User player) {
        gameRoom.addUser(player);
        if (isHomeowner) {
            cleanTask(player);
        }
    }

    @Override
    public void playerInviteFailed(User player) {
        if (isHomeowner) {
            cleanTask(player);
        }
    }

    @Override
    public void playerLeft(User player) {
        gameRoom.removeUser(player);
    }

    @Override
    public void playerReadied(User player) {
        gameRoom.readied(player);
    }

    @Override
    public void roomOpened(GameRoom gameRoom) {
        this.gameRoom = gameRoom;
    }

    @Override
    public void roomClosed() {
        gameRoom = null;
        if (timeoutTask != null) {
            timeoutTask.forEach((k, v) -> v.cancel());
            timeoutTask = null;
        }
    }

    @Override
    public void gameStarted(GameRoom gameRoom) {
        this.gameRoom = gameRoom;
    }

    @Override
    public void playerGameStarted(User user) {
        if (isHomeowner && playerGameStartedCounter.decrementAndGet() == 0) {
            playerGameStartedCounter.set(gameRoom.getNums());
            allPlayersGameStarted();
        }
    }

    @Override
    public void gameEnded() {

    }

    /**
     * 房间内所有玩家已开始游戏
     */
    protected abstract void allPlayersGameStarted();

    private void cleanTask(User player) {
        Timer timer = timeoutTask.get(player.getId());
        if (timer != null) {
            timer.cancel();
        }
    }

}
