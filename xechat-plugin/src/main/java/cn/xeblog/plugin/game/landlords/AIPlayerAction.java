package cn.xeblog.plugin.game.landlords;

import cn.hutool.core.collection.CollectionUtil;
import cn.xeblog.commons.entity.game.landlords.Poker;
import cn.xeblog.commons.entity.game.landlords.PokerInfo;
import cn.xeblog.commons.entity.game.landlords.PokerModel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author anlingyi
 * @date 2022/6/3 3:19 下午
 */
public class AIPlayerAction extends PlayerAction {

    private Map<Integer, List<Poker>> pokersMap;

    private List<Integer> singleList;

    private List<Integer> pairList;

    private List<Integer> threeList;

    private List<Integer> bombList;

    private List<Integer> kingList;

    public AIPlayerAction(PlayerNode playerNode) {
        super(playerNode);
    }

    @Override
    public int callScore(int score) {
        buildPokerModel(pokers);

        int call = 0;
        if (kingList.size() > 1 || bombList.size() > 0) {
            call = 3;
        } else if (kingList.size() == 1 && getPlain() != null) {
            call = 2;
        } else if (kingList.size() == 1 && (getShunzi(true) != null || getShunzi(false) != null
                || getMax(threeList) > 13)) {
            call = 1;
        }

        if (call <= score) {
            call = 0;
        }

        return call;
    }

    @Override
    public PokerInfo processOutPoker(PlayerNode outPlayer, PokerInfo pokerInfo) {
        buildPokerModel(pokers);
        boolean canBomb = false;
        PokerInfo out = null;
        if (pokerInfo != null) {
            if (pokers.size() < pokerInfo.getPokers().size()) {
                return null;
            }

            int outValue;
            switch (pokerInfo.getPokerModel()) {
                case SINGLE:
                    List<Integer> copySingleList = new ArrayList<>(singleList);
                    if (kingList.size() == 1) {
                        copySingleList.add(kingList.get(0));
                    }
                    outValue = getBiggerThanIt(copySingleList, pokerInfo.getValue());
                    if (outValue > 0) {
                        out = PokerUtil.getPokerInfo(pokersMap.get(outValue));
                    }
                    break;
                case PAIR:
                    outValue = getBiggerThanIt(pairList, pokerInfo.getValue());
                    if (outValue > 0) {
                        out = PokerUtil.getPokerInfo(pokersMap.get(outValue));
                    }
                    break;
                case THREE:
                    outValue = getBiggerThanIt(threeList, pokerInfo.getValue());
                    if (outValue > 0) {
                        out = PokerUtil.getPokerInfo(pokersMap.get(outValue));
                    }
                    break;
                case THREE_ONE_SINGLE:
                    outValue = getBiggerThanIt(threeList, pokerInfo.getValue());
                    if (outValue > 0) {
                        List<Poker> outPokers = pokersMap.get(outValue);
                        int minPoker = getMin(singleList);
                        if (minPoker > 0) {
                            outPokers.addAll(pokersMap.get(minPoker));
                        } else {
                            minPoker = getMin(pairList);
                            if (minPoker == 0) {
                                minPoker = getMin(threeList);
                            }
                            if (minPoker > 0) {
                                outPokers.add(pokersMap.get(minPoker).get(0));
                            }
                        }
                        if (outPokers.size() > 0) {
                            out = PokerUtil.getPokerInfo(outPokers);
                        }
                    }
                    break;
                case THREE_ONE_PAIR:
                    outValue = getBiggerThanIt(threeList, pokerInfo.getValue());
                    if (outValue > 0) {
                        List<Poker> outPokers = pokersMap.get(outValue);
                        int minPoker = getMin(pairList);
                        if (minPoker > 0) {
                            outPokers.addAll(pokersMap.get(minPoker));
                        } else {
                            minPoker = getMin(threeList);
                            if (minPoker > 0) {
                                outPokers.addAll(pokersMap.get(minPoker).subList(0, 2));
                            }
                        }
                        if (outPokers.size() > 0) {
                            out = PokerUtil.getPokerInfo(outPokers);
                        }
                    }
                    break;
                case PLAIN_MANNED:
                case PLAIN_UNMANNED:
                    canBomb = true;
                    out = getPlain(pokerInfo);
                    break;
                case SHUN_ZI_PAIR:
                case SHUN_ZI_SINGLE:
                    canBomb = true;
                    out = getShunzi(pokerInfo);
                    break;
                case BOMB:
                    outValue = getBiggerThanIt(bombList, pokerInfo.getValue());
                    if (outValue > 0) {
                        out = PokerUtil.getPokerInfo(pokersMap.get(outValue));
                    }
                    break;
                default:
                    canBomb = true;
                    break;
            }

            if (playerNode.getRole() == 1) {
                if (playerNode.getPokerTotal() < 6) {
                    canBomb = true;
                }
            } else if (playerNode.getPokerTotal() < 10 || playerNode.getNextPlayer().getPokerTotal() < 10
                    || playerNode.getPrevPlayer().getPokerTotal() < 10) {
                canBomb = true;
            }

            if (out == null && canBomb) {
                if (bombList.size() > 0) {
                    out = PokerUtil.getPokerInfo(pokersMap.get(getMin(bombList)));
                }
                if (out == null && kingList.size() > 1) {
                    List<Poker> kingPokerList = new ArrayList<>();
                    kingList.forEach(val -> kingPokerList.add(pokersMap.get(val).get(0)));
                    out = PokerUtil.getPokerInfo(kingPokerList);
                }
            }
        } else {
            out = getPlain();
            if (out == null) {
                out = getShunzi(false);
            }
            if (out == null) {
                out = getShunzi(true);
            }
            if (out == null) {
                out = getThree();
            }
            if (out == null) {
                out = getPair();
            }
            if (out == null) {
                boolean maxed = false;
                if (playerNode.getNextPlayer().getRole() != playerNode.getRole()) {
                    maxed = pokers.size() == 2 && singleList.size() == 2
                            || playerNode.getNextPlayer().getPokerTotal() < 3;
                }
                out = getSingle(maxed);
            }
            if (out == null) {
                out = PokerUtil.getPokerInfo(pokers);
            }
            if (out == null && kingList.size() == 1) {
                out = PokerUtil.getPokerInfo(pokersMap.get(kingList.get(0)));
            }
            if (out == null && bombList.size() > 0) {
                out = PokerUtil.getPokerInfo(pokersMap.get(getMin(bombList)));
            }
            if (out == null) {
                if (kingList.size() == 2) {
                    List<Poker> kingPoker = new ArrayList<>();
                    kingPoker.add(pokersMap.get(16).get(0));
                    kingPoker.add(pokersMap.get(17).get(0));
                    out = PokerUtil.getPokerInfo(kingPoker);
                }
            }
        }

        if (out != null && !out.biggerThanIt(pokerInfo)) {
            return null;
        }

        return out;
    }

    protected void buildPokerModel() {
        buildPokerModel(this.pokers);
    }

    protected void buildPokerModel(List<Poker> pokers) {
        singleList = new ArrayList<>();
        pairList = new ArrayList<>();
        threeList = new ArrayList<>();
        bombList = new ArrayList<>();
        kingList = new ArrayList<>();

        List<Poker> pokerList = new ArrayList<>(pokers);
        pokersMap = pokerList.stream().collect(Collectors.groupingBy(Poker::getValue));
        pokersMap.forEach((k, v) -> {
            if (k > 15) {
                kingList.add(k);
                return;
            }

            int size = v.size();
            if (size == 1) {
                singleList.add(k);
            } else if (size == 2) {
                pairList.add(k);
            } else if (size == 3) {
                threeList.add(k);
            } else {
                bombList.add(k);
            }
        });

        Collections.sort(singleList);
        Collections.sort(pairList);
        Collections.sort(threeList);
        Collections.sort(bombList);
    }

    private int getBiggerThanIt(List<Integer> list, int value) {
        for (Integer val : list) {
            if (val > value) {
                return val;
            }
        }

        return 0;
    }

    private int getMin(List<Integer> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }

        return list.get(0);
    }

    private int getMax(List<Integer> list) {
        int size = CollectionUtil.size(list);
        if (size == 0) {
            return 0;
        }

        return list.get(size - 1);
    }

    private PokerInfo getPlain(PokerInfo pokerInfo) {
        int minValue = pokerInfo.getPokers().get(0).getValue();
        int maxValue = pokerInfo.getValue();
        int shunziLen = maxValue - minValue + 1;
        int size = threeList.size();
        if (size < shunziLen) {
            return null;
        }

        int shunzi = 1;
        List<Integer> shunziPokerValues = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i + 1 == size) {
                break;
            }

            int currValue = threeList.get(i);
            int nexValue = threeList.get(i + 1);
            if (currValue < minValue || currValue > 14 || nexValue > 14) {
                continue;
            }

            if (currValue + 1 == nexValue) {
                if (shunzi == 1) {
                    shunziPokerValues.add(currValue);
                }
                shunziPokerValues.add(nexValue);
                shunzi++;
            } else {
                shunzi = 1;
                shunziPokerValues.clear();
            }

            if (shunzi == shunziLen) {
                break;
            }
        }

        if (shunziPokerValues.size() != shunziLen) {
            return null;
        }

        List<Poker> pokerList = new ArrayList<>();
        shunziPokerValues.forEach(val -> pokerList.addAll(pokersMap.get(val)));
        if (pokerInfo.getPokerModel() == PokerModel.PLAIN_MANNED) {
            int outTotal = pokerInfo.getPokers().size();
            int withTotal = outTotal - shunziLen * 3;
            if (withTotal == shunziLen * 2) {
                int pairTotal = withTotal / 2;
                if (pairList.size() < pairTotal) {
                    return null;
                }

                pairList.subList(0, pairTotal).forEach(val -> pokerList.addAll(pokersMap.get(val)));
            } else {
                int singleSize = singleList.size();
                int surplus = withTotal;
                if (singleSize > 0) {
                    int withSingleSize = Math.min(singleSize, withTotal);
                    surplus -= withSingleSize;
                    singleList.subList(0, withSingleSize).forEach(val -> pokerList.addAll(pokersMap.get(val)));
                }
                if (surplus > 0) {
                    if (pairList.size() > 0) {
                        out:
                        for (Integer val : pairList) {
                            for (Poker poker : pokersMap.get(val)) {
                                pokerList.add(poker);
                                if (--surplus == 0) {
                                    break out;
                                }
                            }
                        }
                    }
                    if (surplus > 0) {
                        List<Integer> copyThreeList = new ArrayList<>(threeList);
                        copyThreeList.removeAll(shunziPokerValues);
                        if (copyThreeList.size() > 0) {
                            out:
                            for (Integer val : copyThreeList) {
                                for (Poker poker : pokersMap.get(val)) {
                                    pokerList.add(poker);
                                    if (--surplus == 0) {
                                        break out;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return PokerUtil.getPokerInfo(pokerList);
    }

    private PokerInfo getShunzi(PokerInfo pokerInfo) {
        boolean isSingle = pokerInfo.getPokerModel() == PokerModel.SHUN_ZI_SINGLE;
        int minValue = pokerInfo.getPokers().get(0).getValue();
        int maxValue = pokerInfo.getValue();
        int shunziLen = maxValue - minValue + 1;
        List<Integer> pokerValue = new ArrayList<>();
        if (isSingle) {
            pokerValue.addAll(singleList);
        }
        pokerValue.addAll(pairList);
        pokerValue.addAll(threeList);
        Collections.sort(pokerValue);

        int size = pokerValue.size();
        if (size < shunziLen) {
            return null;
        }

        int shunzi = 1;
        List<Integer> shunziPokerValues = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i + 1 == size) {
                break;
            }

            int currValue = pokerValue.get(i);
            int nexValue = pokerValue.get(i + 1);
            if (currValue < minValue || currValue > 14 || nexValue > 14) {
                continue;
            }

            if (currValue + 1 == nexValue) {
                if (shunzi == 1) {
                    shunziPokerValues.add(currValue);
                }
                shunziPokerValues.add(nexValue);
                shunzi++;
            } else {
                shunzi = 1;
                shunziPokerValues.clear();
            }

            if (shunzi == shunziLen) {
                break;
            }
        }

        if (shunziPokerValues.size() != shunziLen) {
            return null;
        }

        List<Poker> pokerList = new ArrayList<>();
        shunziPokerValues.forEach(val -> pokerList.addAll(pokersMap.get(val).subList(0, isSingle ? 1 : 2)));
        return PokerUtil.getPokerInfo(pokerList);
    }

    public PokerInfo getPlain() {
        int size = threeList.size();
        if (size < 2) {
            return null;
        }

        List<Integer> maxShunziPokerValues = null;
        int maxShunzi = 0;
        int shunzi = 1;
        List<Integer> shunziPokerValues = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (shunzi > maxShunzi) {
                maxShunzi = shunzi;
                maxShunziPokerValues = shunziPokerValues;
            }
            if (shunzi == 1) {
                if (maxShunzi > 1) {
                    break;
                }

                shunziPokerValues.clear();
            }

            if (i + 1 == size) {
                break;
            }

            int currValue = threeList.get(i);
            int nexValue = threeList.get(i + 1);
            if (currValue > 14 || nexValue > 14) {
                continue;
            }

            if (currValue + 1 == nexValue) {
                if (shunzi == 1) {
                    shunziPokerValues.add(currValue);
                }
                shunziPokerValues.add(nexValue);
                shunzi++;
            } else {
                shunzi = 1;
            }
        }

        if (maxShunzi < 2) {
            return null;
        }

        List<Poker> pokerList = new ArrayList<>();
        maxShunziPokerValues.forEach(val -> pokerList.addAll(pokersMap.get(val)));

        int withTotal = maxShunzi;
        if (pairList.size() >= withTotal) {
            pairList.subList(0, withTotal).forEach(val -> pokerList.addAll(pokersMap.get(val)));
        } else {
            int singleSize = singleList.size();
            int surplus = withTotal;
            if (singleSize > 0) {
                int withSingleSize = Math.min(singleSize, withTotal);
                surplus -= withSingleSize;
                singleList.subList(0, withSingleSize).forEach(val -> pokerList.addAll(pokersMap.get(val)));
            }
            if (surplus > 0) {
                if (pairList.size() > 0) {
                    out:
                    for (Integer val : pairList) {
                        for (Poker poker : pokersMap.get(val)) {
                            pokerList.add(poker);
                            if (--surplus == 0) {
                                break out;
                            }
                        }
                    }
                }
                if (surplus > 0) {
                    List<Integer> copyThreeList = new ArrayList<>(threeList);
                    copyThreeList.removeAll(shunziPokerValues);
                    if (copyThreeList.size() > 0) {
                        out:
                        for (Integer val : copyThreeList) {
                            for (Poker poker : pokersMap.get(val)) {
                                pokerList.add(poker);
                                if (--surplus == 0) {
                                    break out;
                                }
                            }
                        }
                    }
                }
            }
        }

        return PokerUtil.getPokerInfo(pokerList);
    }

    public PokerInfo getShunzi(boolean isSingle) {
        List<Integer> pokerValue = new ArrayList<>();
        if (isSingle) {
            pokerValue.addAll(singleList);
        }
        pokerValue.addAll(pairList);
        pokerValue.addAll(threeList);
        Collections.sort(pokerValue);

        int size = pokerValue.size();
        int shunzi = 1;
        int maxShunzi = 0;
        List<Integer> maxShunziPokerValues = null;
        List<Integer> shunziPokerValues = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (shunzi > maxShunzi) {
                maxShunzi = shunzi;
                maxShunziPokerValues = shunziPokerValues;
            }
            if (shunzi == 1) {
                if (maxShunzi > 4 || !isSingle && maxShunzi > 2) {
                    break;
                }

                shunziPokerValues.clear();
            }

            if (i + 1 == size) {
                break;
            }

            int currValue = pokerValue.get(i);
            int nexValue = pokerValue.get(i + 1);
            if (currValue > 14 || nexValue > 14) {
                continue;
            }

            if (currValue + 1 == nexValue) {
                if (shunzi == 1) {
                    shunziPokerValues.add(currValue);
                }
                shunziPokerValues.add(nexValue);
                shunzi++;
            } else {
                shunzi = 1;
            }
        }

        if (isSingle) {
            if (maxShunzi < 5) {
                return null;
            }
        } else if (maxShunzi < 3) {
            return null;
        }

        List<Poker> pokerList = new ArrayList<>();
        maxShunziPokerValues.forEach(val -> pokerList.addAll(pokersMap.get(val).subList(0, isSingle ? 1 : 2)));
        return PokerUtil.getPokerInfo(pokerList);
    }

    public PokerInfo getThree() {
        int value = getMin(threeList);
        if (value == 0) {
            return null;
        }

        List<Poker> pokerList = pokersMap.get(value);
        if (pairList.size() > 0) {
            pokerList.addAll(pokersMap.get(getMin(pairList)));
        } else if (singleList.size() > 0) {
            pokerList.add(pokersMap.get(getMin(singleList)).get(0));
        }
        return PokerUtil.getPokerInfo(pokerList);
    }

    public PokerInfo getPair() {
        int value = getMin(pairList);
        if (value == 0) {
            return null;
        }

        return PokerUtil.getPokerInfo(pokersMap.get(value));
    }

    public PokerInfo getSingle() {
        return getSingle(false);
    }

    public PokerInfo getSingle(boolean maxed) {
        int value = maxed ? getMax(singleList) : getMin(singleList);
        if (value == 0) {
            return null;
        }

        return PokerUtil.getPokerInfo(pokersMap.get(value));
    }

    public static void main(String[] args) {
//        test1();
        test2();
    }

    private static void test1() {
        // 测试AI出小牌放队友走

        PlayerNode playerNode = new PlayerNode();
        playerNode.setPlayer("1");
        playerNode.setRole(1);
        PlayerNode playerNode2 = new PlayerNode();
        playerNode2.setPlayer("2");
        playerNode2.setRole(1);
        // 队友只剩一张牌了
        playerNode2.setPokerTotal(1);
        PlayerNode playerNode3 = new PlayerNode();
        playerNode3.setPlayer("3");
        playerNode3.setRole(2);

        playerNode.setPrevPlayer(playerNode3);
        playerNode.setNextPlayer(playerNode2);
        playerNode2.setPrevPlayer(playerNode);
        playerNode2.setNextPlayer(playerNode3);
        playerNode3.setPrevPlayer(playerNode2);
        playerNode3.setNextPlayer(playerNode);

        AIPlayerAction playerAction = new AIPlayerAction(playerNode);

        List<Poker> pokers = new ArrayList<>();
        pokers.add(new Poker(3, Poker.Suits.SPADE));
        pokers.add(new Poker(6, Poker.Suits.CLUB));
        pokers.add(new Poker(10, Poker.Suits.CLUB));
        playerAction.setPokers(pokers);
        playerAction.buildPokerModel();

        // AI应该出最小的3放队友走
        System.out.println(playerAction.outPoker(null, null));
        ;
    }

    private static void test2() {
        // 测试AI出大牌压下家地主

        PlayerNode playerNode = new PlayerNode();
        playerNode.setPlayer("1");
        playerNode.setRole(1);
        PlayerNode playerNode2 = new PlayerNode();
        playerNode2.setPlayer("2");
        playerNode2.setRole(2);
        // 地主只剩1张牌了
        playerNode2.setPokerTotal(1);
        PlayerNode playerNode3 = new PlayerNode();
        playerNode3.setPlayer("3");
        playerNode3.setRole(1);

        playerNode.setPrevPlayer(playerNode3);
        playerNode.setNextPlayer(playerNode2);
        playerNode2.setPrevPlayer(playerNode);
        playerNode2.setNextPlayer(playerNode3);
        playerNode3.setPrevPlayer(playerNode2);
        playerNode3.setNextPlayer(playerNode);

        AIPlayerAction playerAction = new AIPlayerAction(playerNode);

        List<Poker> pokers = new ArrayList<>();
        pokers.add(new Poker(3, Poker.Suits.SPADE));
        pokers.add(new Poker(6, Poker.Suits.CLUB));
        pokers.add(new Poker(10, Poker.Suits.CLUB));
        playerAction.setPokers(pokers);
        playerAction.buildPokerModel();

        // AI应该出最大的10
        System.out.println(playerAction.outPoker(null, null));
        ;
    }

    private static void test() {
        List<List<Poker>> allocPokers = PokerUtil.allocPokers();

        PlayerNode playerNode = new PlayerNode();
        playerNode.setPlayer("1");
        PlayerNode playerNode2 = new PlayerNode();
        playerNode2.setPlayer("2");

        List<Poker> pokers = new ArrayList<>();
        pokers.add(new Poker(3, Poker.Suits.SPADE));
        pokers.add(new Poker(3, Poker.Suits.CLUB));
        pokers.add(new Poker(3, Poker.Suits.DIAMOND));
        pokers.add(new Poker(4, Poker.Suits.SPADE));
        pokers.add(new Poker(4, Poker.Suits.DIAMOND));
        pokers.add(new Poker(4, Poker.Suits.CLUB));
        pokers.add(new Poker(5, Poker.Suits.SPADE));
        pokers.add(new Poker(5, Poker.Suits.DIAMOND));
        pokers.add(new Poker(5, Poker.Suits.CLUB));
        pokers.add(new Poker(6, Poker.Suits.HEART));
        pokers.add(new Poker(7, Poker.Suits.SPADE));
        pokers.add(new Poker(8, Poker.Suits.CLUB));
        pokers.add(new Poker(8, Poker.Suits.SPADE));
        pokers.add(new Poker(9, Poker.Suits.CLUB));
        pokers.add(new Poker(9, Poker.Suits.DIAMOND));
        pokers.add(new Poker(10, Poker.Suits.CLUB));
        pokers.add(new Poker(10, Poker.Suits.DIAMOND));

        AIPlayerAction playerAction = new AIPlayerAction(playerNode);
//        playerAction.setPokers(allocPokers.get(0));
//        playerAction.setLastPokers(allocPokers.get(3));
        playerAction.setPokers(pokers);
        playerAction.buildPokerModel();

        System.out.println("手牌：" + playerAction.getPokers());
        System.out.println("叫分" + playerAction.callScore(0));
        System.out.println("飞机：" + playerAction.getPlain());
        System.out.println("联对：" + playerAction.getShunzi(false));
        System.out.println("单顺子：" + playerAction.getShunzi(true));
        System.out.println("三张：" + playerAction.getThree());
        System.out.println("对牌：" + playerAction.getPair());
        System.out.println("单牌：" + playerAction.getSingle());

        System.out.println("出牌：" + playerAction.outPoker(null, null));
    }

}
