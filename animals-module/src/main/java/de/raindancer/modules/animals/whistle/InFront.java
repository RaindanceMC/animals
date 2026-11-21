package de.raindancer.modules.animals.whistle;

/** Where a called horse lands: {@code blocks} ahead of the caller's gaze, turned to face them. */
record InFront(double x, double z, float yaw) {

    static InFront of(double x, double z, float callerYaw, double blocks) {
        double radians = Math.toRadians(callerYaw);
        float facing = callerYaw + 180f;
        while (facing > 180f) {
            facing -= 360f;
        }
        while (facing <= -180f) {
            facing += 360f;
        }
        return new InFront(x - Math.sin(radians) * blocks, z + Math.cos(radians) * blocks, facing);
    }
}
