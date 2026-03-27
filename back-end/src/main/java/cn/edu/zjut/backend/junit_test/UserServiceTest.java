package cn.edu.zjut.backend.junit_test;

import cn.edu.zjut.backend.service.UserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class UserServiceTest {

    //测试 add()方法

    /**
     * 步骤二：声明变量
     */
    private String username;
    private String password;
    private Boolean expectedResult;
    private UserService userService;

    public UserServiceTest(String username, String password,Boolean expectedResult) {
        this.username = username;
        this.password = password;
        this.expectedResult = expectedResult;
    }

    /**
         * 步骤三：为测试类声明一个带有参数的公共构造函数，为变量赋值
         */



        /**
         * 步骤四：为测试类声明一个使用注解 org.junit.runners.Parameterized.Parameters 修饰的，返回值为
         * java.util.Collection 的公共静态方法，并在此方法中初始化所有需要测试的参数对
         *   1）该方法必须由Parameters注解修饰
         2）该方法必须为public static的
         3）该方法必须返回Collection类型
         4）该方法的名字不做要求
         5）该方法没有参数
         */
        @Parameterized.Parameters
        public static Collection primeNumbers() {
            return Arrays.asList(new Object[][]{
                    {"admin", "admin123", false},
                    {"admin", "admin123456", true},
            });
        }
        @Before
        public void initialize() {
            userService = new UserService();
        }

        @Test
        public void testLogin(){

            boolean res = userService.login(username, password) != null;
            Assert.assertEquals(res, expectedResult);
        }


}
