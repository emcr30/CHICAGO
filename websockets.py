"""
Cliente WebSocket de ejemplo para app móvil
Simula cómo la app móvil se conectaría al servidor
"""
import asyncio
import websockets
import json
from datetime import datetime
import requests

class SecurityAlertsClient:
    def __init__(self, user_id: str, server_url: str = "ws://localhost:8000"):
        self.user_id = user_id
        self.server_url = f"{server_url}/ws/{user_id}"
        self.api_url = server_url.replace("ws://", "http://").replace("wss://", "https://")
        self.websocket = None
        self.running = False
    
    async def connect(self):
        """Conecta al servidor WebSocket"""
        try:
            self.websocket = await websockets.connect(self.server_url)
            self.running = True
            print(f"✅ Conectado como {self.user_id}")
        except Exception as e:
            print(f"❌ Error al conectar: {e}")
            return False
        return True
    
    async def send_location(self, latitude: float, longitude: float):
        """Envía ubicación actual al servidor"""
        if not self.websocket:
            return
        
        message = {
            "type": "location_update",
            "latitude": latitude,
            "longitude": longitude,
            "timestamp": datetime.utcnow().isoformat()
        }
        
        await self.websocket.send(json.dumps(message))
        print(f"📍 Ubicación enviada: ({latitude}, {longitude})")
    
    async def listen_for_alerts(self):
        """Escucha alertas en tiempo real"""
        try:
            async for message in self.websocket:
                data = json.loads(message)
                await self.handle_message(data)
        except websockets.exceptions.ConnectionClosed:
            print("⚠️ Conexión cerrada")
            self.running = False
        except Exception as e:
            print(f"❌ Error al escuchar: {e}")
            self.running = False
    
    async def handle_message(self, data: dict):
        """Procesa mensajes recibidos del servidor"""
        msg_type = data.get("type")
        
        if msg_type == "connection":
            print(f"🔗 {data.get('message')}")
        
        elif msg_type == "new_alert":
            alert = data.get("alert", {})
            print(f"\n🚨 NUEVA ALERTA:")
            print(f"   Tipo: {alert.get('primary_type')}")
            print(f"   Descripción: {alert.get('description')}")
            print(f"   Ubicación: ({alert.get('latitude')}, {alert.get('longitude')})")
            print(f"   Hora: {alert.get('timestamp')}\n")
            
            # Aquí la app móvil mostraría una notificación push
            self.show_notification(alert)
        
        elif msg_type == "location_confirmed":
            print(f"✓ {data.get('message')}")
        
        elif msg_type == "pong":
            print("💓 Keepalive OK")
    
    def show_notification(self, alert: dict):
        """Simula notificación push en móvil"""
        print("=" * 50)
        print("📱 NOTIFICACIÓN PUSH")
        print(f"🚨 {alert.get('primary_type')}")
        print(f"📍 {alert.get('description')}")
        print("=" * 50)
    
    async def send_ping(self):
        """Envía ping para mantener conexión activa"""
        while self.running:
            try:
                await self.websocket.send(json.dumps({"type": "ping"}))
                await asyncio.sleep(30)  # Ping cada 30 segundos
            except:
                break
    
    def get_nearby_alerts(self, latitude: float, longitude: float, radius_km: float = 2.0):
        """Obtiene alertas cercanas vía API REST"""
        try:
            response = requests.post(
                f"{self.api_url}/api/alerts/nearby",
                json={
                    "latitude": latitude,
                    "longitude": longitude,
                    "radius_km": radius_km,
                    "limit": 50
                },
                headers={"Authorization": "Bearer user_token"},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                print(f"\n📍 Alertas cercanas (radio {radius_km}km):")
                print(f"   Total: {data['count']}")
                
                for alert in data['alerts'][:5]:  # Mostrar primeras 5
                    print(f"   - {alert['primary_type']}: {alert.get('distance_km', 0):.2f}km")
                
                return data['alerts']
            else:
                print(f"❌ Error al obtener alertas: {response.status_code}")
                return []
        
        except Exception as e:
            print(f"❌ Error: {e}")
            return []
    
    async def run(self, user_latitude: float, user_longitude: float):
        """Ejecuta el cliente de forma continua"""
        if not await self.connect():
            return
        
        # Enviar ubicación inicial
        await self.send_location(user_latitude, user_longitude)
        
        # Obtener alertas cercanas
        self.get_nearby_alerts(user_latitude, user_longitude, radius_km=2.0)
        
        # Crear tareas concurrentes
        listen_task = asyncio.create_task(self.listen_for_alerts())
        ping_task = asyncio.create_task(self.send_ping())
        
        try:
            await asyncio.gather(listen_task, ping_task)
        except KeyboardInterrupt:
            print("\n👋 Desconectando...")
        finally:
            if self.websocket:
                await self.websocket.close()
            self.running = False


# Ejemplo de uso
async def main():
    print("""
    ╔═══════════════════════════════════════════════════════════╗
    ║     📱 Cliente de Alertas de Seguridad - Arequipa        ║
    ╠═══════════════════════════════════════════════════════════╣
    ║  Simula una app móvil conectada al sistema de alertas   ║
    ╚═══════════════════════════════════════════════════════════╝
    """)
    
    # Simular usuario en el Centro Histórico de Arequipa
    user_id = "user_001"
    user_lat = -16.424060  # Centro Histórico
    user_lon = -71.556775
    
    print(f"👤 Usuario: {user_id}")
    print(f"📍 Ubicación inicial: ({user_lat}, {user_lon})")
    print(f"🔗 Conectando al servidor...\n")
    
    client = SecurityAlertsClient(user_id=user_id)
    
    try:
        await client.run(user_lat, user_lon)
    except Exception as e:
        print(f"❌ Error: {e}")


if __name__ == "__main__":
    print("Presiona Ctrl+C para salir\n")
    asyncio.run(main())