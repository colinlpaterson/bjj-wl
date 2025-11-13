package io;

import model.Workout;
import java.util.List;

public interface WorkoutRepository {
    void append(Workout w) throws Exception;
    List<String> readAllJsonLines() throws Exception; // raw lines for now (simple)
    void replaceLine(int index, Workout replacement) throws Exception; // update by index
}

