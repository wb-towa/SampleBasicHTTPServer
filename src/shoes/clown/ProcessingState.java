package shoes.clown;


/**
 * The various states for the processing thread.
 * NEW      - never ran a job.
 * IDLE     - in between jobs (like that one uncle)
 * WORKING  - currently processing a job
 * FINISHED - Processing thread has been signaled to stop
 */
public enum ProcessingState {
    NEW, IDLE, WORKING, FINISHED
}
