package scenes.gta;

/** The player's money. Simple balance with add/spend. */
public class Economy {

    private int money;

    public void add(int amount) {
        money += amount;
    }

    /** Spend if affordable; returns true on success. */
    public boolean spend(int amount) {
        if (money >= amount) {
            money -= amount;
            return true;
        }
        return false;
    }

    public int money() {
        return money;
    }
}
