package fl.wearable.autosport.menu;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;
import fl.wearable.autosport.R;
import me.samlss.timomenu.TimoItemViewParameter;

public class MenuHelper {
    private MenuHelper(){

    }


    public static int ROW_TEXT[][] = {
            {R.string.crosstrainer, R.string.rowing, R.string.jumping, R.string.unknown},
    };

    public static List<TimoItemViewParameter> getTopList(int itemWidth){
        List<TimoItemViewParameter> listTop = new ArrayList<>();

        TimoItemViewParameter crosstrainer = getTimoItemViewParameter(itemWidth, R.drawable.crosstrainer,
                R.drawable.crosstrainer, R.string.crosstrainer, R.color.normal_text_color,
                R.color.highlight_text_color);

        TimoItemViewParameter rowing = getTimoItemViewParameter(itemWidth, R.drawable.rowing,
                R.drawable.rowing, R.string.rowing, R.color.normal_text_color,
                R.color.highlight_text_color);

        TimoItemViewParameter jumping = getTimoItemViewParameter(itemWidth, fl.wearable.autosport.R.drawable.jumping,
                R.drawable.jumping, R.string.jumping, R.color.normal_text_color,
                R.color.highlight_text_color);


        listTop.add(crosstrainer);
        listTop.add(rowing);
        listTop.add(jumping);
        return listTop;
    }

    /*public static List<TimoItemViewParameter> getBottomList(int itemWidth){
        List<TimoItemViewParameter> listBottom = new ArrayList<>();


        TimoItemViewParameter reset = getTimoItemViewParameter(itemWidth, fl.wearable.autosport.R.drawable.btn_reset_normal_200,
                fl.wearable.autosport.R.drawable.btn_reset_pressed_200, fl.wearable.autosport.R.string.run, fl.wearable.autosport.R.color.circular_button_normal,
                fl.wearable.autosport.R.color.colorPrimaryDark);

        TimoItemViewParameter app = getTimoItemViewParameter(itemWidth, R.drawable.common_google_signin_btn_icon_dark,
                R.drawable.common_google_signin_btn_icon_light, R.string.app_name, R.color.ambient_mode_text,
                R.color.button_icon_color);


        listBottom.add(reset);
        listBottom.add(app);
        return listBottom;
    }*/

    public static TimoItemViewParameter getTimoItemViewParameter(int itemWidth,
                                                                 int normalImageRes,
                                                                 int highlightImageRes,
                                                                 int normalTextRes,
                                                                 int normalTextColorRes,
                                                                 int highlightTextColorRes){
        return new TimoItemViewParameter.Builder()
                .setWidth(itemWidth)
                .setImagePadding(new Rect(10, 10, 10, 10))
                .setTextPadding(new Rect(5, 0, 5, 0))
                .setNormalImageRes(normalImageRes)
                .setHighlightedImageRes(highlightImageRes)
                .setNormalTextRes(normalTextRes)
                .setNormalTextColorRes(normalTextColorRes)
                .setHighlightedTextColorRes(highlightTextColorRes)
                .build();

    }


    public static List<String> getAllOpenAnimationName(){
        List<String> list = new ArrayList<>();
        list.add("Flip");
        list.add("Scale");
        list.add("Bomb");
        list.add("Stand Up");
        list.add("Bounce");
        list.add("Bounce In Down");
        list.add("Bounce In Up");
        list.add("Rotate");
        return list;
    }


}
