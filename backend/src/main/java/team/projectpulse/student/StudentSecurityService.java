package team.projectpulse.student;

import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Answers the questions a route asks about one named student: is that student the caller herself
 * ({@code isStudentSelf}), and is the caller an instructor of that student's course section
 * ({@code isCurrentUserInstructorOfStudentSection}), and may the caller deactivate or reactivate that student
 * ({@code canManageStudentLifecycle}).
 *
 * <p>Every check fails closed: an id that names no row, or a link not yet established, is denied rather than
 * throwing. These methods run inside an {@code AuthorizationManager}, upstream of the {@code DispatcherServlet},
 * so an exception thrown here escapes the filter chain as a 500 that also tells the caller which ids exist.
 */
@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;


    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Check if the user is the instructor of the section that the student is in
     *
     * @param studentId the id of the student we need to access, this is NOT the current user's Id
     * @return
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(instructorIdFromJwt));
    }

    /**
     * Whether the caller may deactivate or reactivate the student (BR-student-lifecycle): an instructor of her
     * course section, or the course admin of its course, whether or not that admin is assigned to the course
     * section. Never the student herself. An unknown student, or a course section with no course or course admin,
     * is a denial.
     *
     * @param studentId the id of the student being deactivated, NOT the current user's id
     */
    public boolean canManageStudentLifecycle(Integer studentId) {
        Integer callerId = this.userUtils.getUserId();
        if (callerId == null || studentId.equals(callerId)) {
            return false;
        }
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        Section section = student.getSection();
        if (section.getInstructors().stream().anyMatch(instructor -> callerId.equals(instructor.getId()))) {
            return true;
        }
        Course course = section.getCourse();
        Instructor courseAdmin = course == null ? null : course.getCourseAdmin();
        return courseAdmin != null && callerId.equals(courseAdmin.getId());
    }

}
