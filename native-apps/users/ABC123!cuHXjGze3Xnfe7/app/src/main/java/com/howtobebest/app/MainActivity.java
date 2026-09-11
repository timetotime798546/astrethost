package com.howtobebest.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private TextView tvWisdom;
    private Button btnGenerateWisdom;
    private int lastIndex = -1;

    private static final String[] MASTER_TIPS = {
        "Prioritize consistency over intensity. Going to the gym or studying for 30 minutes every day yields more progress than 5 hours once a week.",
        "Embrace absolute ownership of your outcomes. Avoid shifting blame. When you accept total responsibility, you acquire total power to improve.",
        "Practice single-tasking. Deep focus on one single goal for 90 minutes beats multitasking for an entire afternoon.",
        "Get comfortable being uncomfortable. The highest level of growth resides just outside your current comfort zone.",
        "Compete only with your former self. Compare your progress today against who you were yesterday, not against someone else's highlight reel.",
        "Create a strict morning or evening routine. Habits automate success, conserving cognitive energy for high-impact decision-making.",
        "Invest heavily in your education, skills, and health. These assets appreciate over time and cannot be taken away.",
        "Learn to say 'No' to distractions. Saying yes to non-essential activities means saying no to your core lifetime vision.",
        "Sleep, hydrate, and move. Your body is the engine of your mind; neglecting physiological fundamentals bottlenecks your intellectual potential.",
        "Ship your work early and iterate often. Performed actions provide direct, realistic feedback; abstract planning only generates hypotheticals."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWisdom = (TextView) findViewById(R.id.tv_wisdom);
        btnGenerateWisdom = (Button) findViewById(R.id.btn_generate_wisdom);

        btnGenerateWisdom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayRandomTip();
            }
        });
    }

    private void displayRandomTip() {
        Random random = new Random();
        int index;
        
        // Prevent repeating the same wisdom consecutively if possible
        if (MASTER_TIPS.length > 1) {
            do {
                index = random.nextInt(MASTER_TIPS.length);
            } while (index == lastIndex);
        } else {
            index = 0;
        }

        lastIndex = index;
        tvWisdom.setText(MASTER_TIPS[index]);
    }
}