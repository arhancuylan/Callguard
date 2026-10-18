package com.callguard;

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Run on an emulator. Exercises row semantics without opening settings or touching Bluetooth. */
@RunWith(AndroidJUnit4.class)
public class AdbGuideRowTest {
    @Test public void incompleteRowsExposeOneAccessibleActionAndCompletedRowsStayInformational(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{
                AdbGuide guide=new AdbGuide(activity,null,()->{},()->{},message->{});
                try{
                    Method step=AdbGuide.class.getDeclaredMethod("step",LinearLayout.class,String.class,String.class,boolean.class,boolean.class,Runnable.class);
                    step.setAccessible(true);
                    LinearLayout panel=new LinearLayout(activity);
                    activity.setContentView(panel);
                    AtomicInteger calls=new AtomicInteger();
                    step.invoke(guide,panel,"Kablosuz hata ayıklama","Ayarı aç.",false,true,(Runnable)calls::incrementAndGet);
                    View row=panel.getChildAt(0);
                    assertTrue(row.isClickable());assertTrue(row.isFocusable());
                    assertTrue(row.getMinimumHeight()>=Math.round(48*activity.getResources().getDisplayMetrics().density));
                    AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();
                    assertEquals(Button.class.getName(),node.getClassName().toString());
                    assertTrue("Attached row must expose click semantics: "+node,node.isClickable());
                    assertTrue("Accessible row label: "+node.getContentDescription(),node.getContentDescription().toString().contains("İlgili adımı aç"));
                    assertTrue(row.performClick());assertEquals(1,calls.get());
                    step.invoke(guide,panel,"Doğrulandı","Canlı kanıt.",true,false,(Runnable)calls::incrementAndGet);
                    assertFalse(panel.getChildAt(1).isClickable());assertEquals(1,calls.get());
                }catch(ReflectiveOperationException error){throw new AssertionError(error);}
                finally{guide.close();}
            });
        }
    }
}
