const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Cloud Function que se ejecuta diariamente para eliminar cuentas no verificadas
 * después de 10 días de registro
 */
exports.deleteUnverifiedAccounts = functions.pubsub
    .schedule("every 24 hours")
    .onRun(async (context) => {
      const tenDaysAgo = new Date();
      tenDaysAgo.setDate(tenDaysAgo.getDate() - 10);

      try {
        // Solo aplicar a usuarios que tienen campo emailVerificado (excluye BoteBioWay y Maestro)
        const collections = ["Brindador", "Recolector", "CentroAcopio"];

        for (const collection of collections) {
          const snapshot = await admin.firestore()
              .collection(collection)
              .where("emailVerificado", "==", false)
              .where("fechaRegistro", "<=", tenDaysAgo)
              .get();

          console.log(`Encontrados ${snapshot.size} usuarios no verificados en ${collection}`);

          const batch = admin.firestore().batch();
          const authDeletePromises = [];

          snapshot.forEach((doc) => {
            const userId = doc.data().userId;

            // Eliminar de Firestore
            batch.delete(doc.ref);

            // Eliminar de Firebase Auth
            authDeletePromises.push(
                admin.auth().deleteUser(userId)
                    .then(() => console.log(`Usuario ${userId} eliminado de Auth`))
                    .catch((error) => console.error(`Error al eliminar ${userId}:`, error)),
            );
          });

          // Ejecutar batch de Firestore
          if (snapshot.size > 0) {
            await batch.commit();
            console.log(`${snapshot.size} documentos eliminados de ${collection}`);
          }

          // Esperar a que se eliminen de Auth
          await Promise.all(authDeletePromises);
        }

        console.log("✅ Limpieza de cuentas no verificadas completada");
        return null;
      } catch (error) {
        console.error("❌ Error en limpieza:", error);
        throw error;
      }
    });
