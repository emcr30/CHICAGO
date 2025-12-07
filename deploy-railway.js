#!/usr/bin/env node

const { exec } = require("child_process");
const fs = require("fs");
const path = require("path");

const token = "5514780c-f014-4bd2-842b-a78baebef173";
const projectName = "CHICAGO";
const railwayConfigDir = path.join(process.env.HOME, ".railway");

// Asegurar que el directorio existe
if (!fs.existsSync(railwayConfigDir)) {
  fs.mkdirSync(railwayConfigDir, { recursive: true });
}

// Crear archivo de configuración
const configPath = path.join(railwayConfigDir, "railway.json");
const config = {
  token: token,
};

fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
console.log(`✅ Configuración guardada en ${configPath}`);

// Intentar inicializar proyecto
console.log("\n🚀 Inicializando proyecto CHICAGO en Railway...");

exec(`cd "${process.cwd()}" && railway init -n "${projectName}"`, (error, stdout, stderr) => {
  if (error) {
    console.error(`❌ Error: ${error.message}`);
    console.error(`stderr: ${stderr}`);
    process.exit(1);
  }
  console.log("✅ Proyecto inicializado");
  console.log(stdout);

  // Agregar servicio de base de datos
  console.log("\n📦 Agregando PostgreSQL...");
  exec(`cd "${process.cwd()}" && railway add postgres`, (error, stdout, stderr) => {
    if (error) {
      console.error(`❌ Error al agregar PostgreSQL: ${error.message}`);
    } else {
      console.log("✅ PostgreSQL agregado");
      console.log(stdout);
    }

    // Desplegar
    console.log("\n🚀 Desplegando aplicación...");
    exec(`cd "${process.cwd()}" && railway up`, (error, stdout, stderr) => {
      if (error) {
        console.error(`❌ Error en despliegue: ${error.message}`);
      } else {
        console.log("✅ Despliegue completado!");
        console.log(stdout);
      }
    });
  });
});
