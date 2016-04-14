package complete;

import java.util.ArrayList;
import java.util.List;

public class DeopleteHelper {
    private List<DeopleteItem> items;

    public DeopleteHelper() {
        items = new ArrayList<DeopleteItem>();
    }

    public void add(String word, String info, String kind) {
        for (DeopleteItem i : items) {
            if (i.getWord().equals(word)) {
                i.appendInfo(word + info);
                break;
            }
        }
        items.add(new DeopleteItem(word, word + info, kind));
    }

    public DeopleteItem[] getItems() {
        return items.toArray(new DeopleteItem[]{});
    }
}
