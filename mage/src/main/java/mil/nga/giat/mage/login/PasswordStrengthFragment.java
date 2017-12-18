package mil.nga.giat.mage.login;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 12/15/17.
 */

public class PasswordStrengthFragment extends Fragment {

    private Context context;

    private Zxcvbn zxcvbn = new Zxcvbn();
    private List<String> sanitizedList = new ArrayList<>();

    private TextView passwordStrenghText;
    private ProgressBar passwordStrenghProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_password_strength, container, false);

        passwordStrenghText = (TextView) view.findViewById(R.id.password_strength_text);
        passwordStrenghProgressBar = (ProgressBar) view.findViewById(R.id.password_strength_progress_bar);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public void setSanitizedList(List<String> sanitizedList) {
        this.sanitizedList = sanitizedList;
    }

    public void onPasswordChanged(String password) {
        measurePasswordStrengh(password);
    }

    private void measurePasswordStrengh(String password) {
        Strength strength = zxcvbn.measure(password, sanitizedList);
        int score = strength.getScore();
        passwordStrenghProgressBar.setProgress(score + 1);

        int color = ContextCompat.getColor(context, android.R.color.darker_gray);
        switch (score) {
            case 0: {
                color = ContextCompat.getColor(context, R.color.md_red_500);
                passwordStrenghText.setText("Weak");
                break;
            }
            case 1: {
                color = ContextCompat.getColor(context, R.color.md_orange_500);
                passwordStrenghText.setText("Fair");
                break;
            }
            case 2: {
                color = ContextCompat.getColor(context, R.color.md_amber_500);
                passwordStrenghText.setText("Good");
                break;
            }
            case 3: {
                color = ContextCompat.getColor(context, R.color.md_blue_500);
                passwordStrenghText.setText("Strong");
                break;
            }
            case 4: {
                color = ContextCompat.getColor(context, R.color.md_green_500);
                passwordStrenghText.setText("Excellent");
                break;
            }
        }

        passwordStrenghText.setTextColor(color);
        passwordStrenghProgressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}