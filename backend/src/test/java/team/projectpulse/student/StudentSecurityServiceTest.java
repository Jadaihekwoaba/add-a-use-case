package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * BR-student-lifecycle: who may deactivate or reactivate a student.
 */
@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;

    Course course;

    Student john;

    @BeforeEach
    void setUp() {
        Instructor sectionInstructor = new Instructor("bill", "Bill", "Gates", "b.gates@abc.edu", "123456", true, "instructor");
        sectionInstructor.setId(2);
        Instructor courseAdmin = new Instructor("bingyang", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        courseAdmin.setId(1);

        this.course = new Course("COSC 40993 Senior Design", "Senior design");
        this.course.setCourseAdmin(courseAdmin);

        Section section = new Section("2023-2024", LocalDate.of(2023, 7, 31), LocalDate.of(2024, 10, 6), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        section.addInstructor(sectionInstructor); // The course admin is NOT assigned to this course section
        this.course.addSection(section);

        this.john = new Student("john", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        this.john.setId(4);
        section.addStudent(this.john);
    }

    @Test
    @DisplayName("An instructor of the student's course section may")
    void testInstructorOfCourseSection() {
        given(this.userUtils.getUserId()).willReturn(2);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.canManageStudentLifecycle(4)).isTrue();
    }

    @Test
    @DisplayName("The course admin of its course may, even when not assigned to the course section")
    void testUnassignedCourseAdmin() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.canManageStudentLifecycle(4)).isTrue();
    }

    @Test
    @DisplayName("3a: an instructor of another course section may not")
    void testInstructorOfAnotherCourseSection() {
        given(this.userUtils.getUserId()).willReturn(3);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.canManageStudentLifecycle(4)).isFalse();
    }

    @Test
    @DisplayName("3a: the student herself may not")
    void testStudentHerself() {
        given(this.userUtils.getUserId()).willReturn(4);

        assertThat(this.studentSecurityService.canManageStudentLifecycle(4)).isFalse();
    }

    @Test
    @DisplayName("5c: an unknown student is a denial, not an exception")
    void testUnknownStudent() {
        given(this.userUtils.getUserId()).willReturn(2);
        given(this.studentRepository.findById(999)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.canManageStudentLifecycle(999)).isFalse();
    }

    @Test
    @DisplayName("A course section with no course is a denial, not an exception")
    void testCourseSectionWithNoCourse() {
        Section orphanSection = new Section("Orphan", LocalDate.of(2023, 7, 31), LocalDate.of(2024, 10, 6), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        Student jana = new Student("jana", "Jana", "Norton", "j.norton@abc.edu", "123456", true, "student");
        jana.setId(5);
        orphanSection.addStudent(jana);
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(5)).willReturn(Optional.of(jana));

        assertThat(this.studentSecurityService.canManageStudentLifecycle(5)).isFalse();
    }

}
