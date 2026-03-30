package cn.edu.zjut.backend.junit_test;

import cn.edu.zjut.backend.po.Course;
import cn.edu.zjut.backend.service.CourseService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class CourseServiceTest {

    private CourseService courseService;

    // 根据你数据库实际值填写（已根据截图修改）
    private static final Long TEACHER_ID = 2L;      // 你的 teacher_id = 2
    private static final Long ACADEMIC_ID = 3L;     // 你的 academic_id = 3

    @Before
    public void setUp() {
        courseService = new CourseService();
    }

    // ====================== 1. 创建课程测试 ======================
    @Test
    public void testCreateCourse_Success() {
        Course course = new Course();
        course.setCourseName("测试新建课程-成功案例");
        course.setTeacherId(TEACHER_ID);
        course.setSubjectId(1);                    // 必须是 Integer，不要加 L
        course.setDescription("这是一个JUnit测试创建的课程");

        String result = courseService.createCourse(course);
        Assert.assertEquals("创建课程成功应返回空字符串", "", result);
    }

    @Test
    public void testCreateCourse_SubjectNotExist() {
        Course course = new Course();
        course.setCourseName("测试新建课程-学科不存在");
        course.setTeacherId(TEACHER_ID);
        course.setSubjectId(99999);

        String result = courseService.createCourse(course);
        Assert.assertTrue("学科不存在时应返回错误提示",
                result != null && result.contains("学科"));
    }

    // ====================== 2. 查询类测试 ======================
    @Test
    public void testFindCourseById_Success() {
        Course course = courseService.findCourseById(101L);
        Assert.assertNotNull("已存在的课程ID应返回课程对象", course);
    }

    @Test
    public void testFindCourseById_NotFound() {
        Course course = courseService.findCourseById(99999L);
        Assert.assertNull("不存在的课程ID应返回 null", course);
    }

    @Test
    public void testFindCourseByInviteCode_Success() {
        Course course = courseService.findCourseByInviteCode("ABC123");
        Assert.assertNotNull("使用存在的邀请码应找到课程", course);
    }

    @Test
    public void testFindCourseByInviteCode_NotFound() {
        Course course = courseService.findCourseByInviteCode("NOTEXIST999");
        Assert.assertNull("不存在的邀请码应返回 null", course);
    }

    @Test
    public void testGetCoursesByTeacher() {
        List<Course> courses = courseService.getCoursesByTeacher(TEACHER_ID);
        Assert.assertNotNull("查询教师课程列表不应为 null", courses);
    }

    // ====================== 3. 权限与边界测试 ======================
    @Test
    public void testUpdateCourseById_NoPermission() {
        boolean result = courseService.updateCourseById(101L, 99999L, "无权限测试", null);
        Assert.assertFalse("无权限修改应返回 false", result);
    }

    @Test
    public void testUpdateCourseStatus_InvalidStatus() {
        boolean result = courseService.updateCourseStatus(101L, TEACHER_ID, "2");
        Assert.assertFalse("无效状态值应返回 false", result);
    }

    @Test
    public void testDeleteCourseById_NotExist() {
        boolean result = courseService.deleteCourseById(99999L, TEACHER_ID);
        Assert.assertFalse("课程不存在应返回 false", result);
    }

    @Test
    public void testDeleteCourseById_NoPermission() {
        boolean result = courseService.deleteCourseById(103L, TEACHER_ID);
        Assert.assertFalse("无权限删除应返回 false", result);
    }
}