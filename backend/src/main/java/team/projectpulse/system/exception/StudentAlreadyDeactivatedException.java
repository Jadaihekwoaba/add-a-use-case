package team.projectpulse.system.exception;

/**
 * The student named by a deactivation request is already deactivated (UC-STU-deactivate-student, extension 5b).
 * Mapped to 409 rather than treated as idempotent, so the caller is never told "this student has been
 * deactivated" by a request that changed nothing.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("This student is already deactivated.");
    }

}
