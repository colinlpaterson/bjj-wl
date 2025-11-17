package io;

import model.Workout;
import java.util.List;

public interface WorkoutRepository {
    void append(Workout w) throws Exception;

    List<String> readAllJsonLines() throws Exception;

    void replaceLine(int index, Workout replacement) throws Exception;

    // NEW: delete a workout at the given line index
    void deleteLine(int index) throws Exception;
}

