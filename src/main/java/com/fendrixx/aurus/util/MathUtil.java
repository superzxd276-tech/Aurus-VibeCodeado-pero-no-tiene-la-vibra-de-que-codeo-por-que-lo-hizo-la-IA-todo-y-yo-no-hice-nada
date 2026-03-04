package com.fendrixx.aurus.util;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class MathUtil {

    // Constante pre-calculada para evitar multiplicaciones extra
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    // FOLIA SUPPORT: Cada hilo del servidor tendrá su propia caché de expresiones.
    // Esto evita que "exp4j" re-compile los Strings a cada rato y evita atascos de concurrencia.
    private static final ThreadLocal<Map<String, Expression>> EXPRESSION_CACHE = ThreadLocal.withInitial(HashMap::new);

    /**
     * Mantenido para API externa, pero ahora resuelto con matemática directa.
     * Cero normalizaciones y cero productos cruzados en tiempo de ejecución.
     */
    public static Vector[] getCameraBasis(float yaw, float pitch) {
        double y = yaw * DEG_TO_RAD;
        double p = pitch * DEG_TO_RAD;

        double cosY = Math.cos(y);
        double sinY = Math.sin(y);
        double cosP = Math.cos(p);
        double sinP = Math.sin(p);

        Vector forward = new Vector(-sinY * cosP, -sinP, cosY * cosP);
        Vector right = new Vector(-cosY, 0, -sinY);
        Vector up = new Vector(-sinY * sinP, cosP, cosY * sinP);

        return new Vector[]{forward, right, up};
    }

    public static Location getMenuOrigin(Location eyeLoc, float yaw, float pitch, double distance) {
        double y = yaw * DEG_TO_RAD;
        double p = pitch * DEG_TO_RAD;

        // Cero Vectores temporales, mutamos las coordenadas de la localización clonada directamente
        double dx = -Math.sin(y) * Math.cos(p) * distance;
        double dy = -Math.sin(p) * distance;
        double dz = Math.cos(y) * Math.cos(p) * distance;

        return eyeLoc.clone().add(dx, dy, dz);
    }

    public static Location calculateComponentLocation(Location origin, float yaw, float pitch, double x, double y) {
        double yawRad = yaw * DEG_TO_RAD;
        double pitchRad = pitch * DEG_TO_RAD;

        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double cosP = Math.cos(pitchRad);
        double sinP = Math.sin(pitchRad);

        // Algoritmo analítico de Right * x + Up * y sin instanciar Vectores
        double dx = (-cosY * x) + (-sinY * sinP * y);
        double dy = (0 * x) + (cosP * y); // el 0*x es intencional por fórmula pura (elimina cálculo Right_Y)
        double dz = (-sinY * x) + (cosY * sinP * y);

        Location loc = origin.clone().add(dx, dy, dz);
        loc.setYaw(yaw + 180f);
        loc.setPitch(-pitch);

        return loc;
    }

    public static Location getCursorLocation(Location origin, float cameraYaw, float cameraPitch,
                                             float playerYaw, float playerPitch, double distance) {

        float dYaw = normalizeAngle(playerYaw - cameraYaw);
        float dPitch = normalizeAngle(playerPitch - cameraPitch);

        double cx = Math.tan(dYaw * DEG_TO_RAD) * distance;
        double cy = -Math.tan(dPitch * DEG_TO_RAD) * distance;

        // Reutilizamos el método de arriba para evitar duplicar las matemáticas
        return calculateComponentLocation(origin, cameraYaw, cameraPitch, cx, cy);
    }

    public static float normalizeAngle(float angle) {
        // En vez de un bucle while infinito, usamos la operación módulo (inmensamente más rápido)
        angle = angle % 360f;
        if (angle <= -180f) angle += 360f;
        if (angle > 180f) angle -= 360f;
        return angle;
    }

    public static double evaluate(String formula, double t) {
        try {
            Map<String, Expression> cache = EXPRESSION_CACHE.get();
            Expression expr = cache.get(formula);

            // Si la fórmula matemática nunca se ha compilado en este hilo, la construimos y guardamos
            if (expr == null) {
                expr = new ExpressionBuilder(formula).variable("t").build();
                cache.put(formula, expr);
            }

            // Reutilizamos la misma expresión inyectándole el nuevo valor de 't'.
            return expr.setVariable("t", t).evaluate();
        } catch (Exception e) {
            return 0; // Fallback seguro
        }
    }
}
