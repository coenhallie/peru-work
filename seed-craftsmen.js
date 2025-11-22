const admin = require('firebase-admin');

// Initialize Firebase Admin
// You need to download the service account key from Firebase Console:
// 1. Go to Firebase Console > Project Settings > Service Accounts
// 2. Click "Generate New Private Key"
// 3. Save the file as 'serviceAccountKey.json' in the project root
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'workapp-76f52'
});

const db = admin.firestore();

const craftsmen = [
  {
    name: "Carlos Mendoza",
    craft: "Carpintería",
    rating: 4.8,
    profileImageUrl: "https://randomuser.me/api/portraits/men/1.jpg",
    experience: 15,
    description: "Carpintero experimentado especializado en muebles a medida y restauración",
    bio: "Con más de 15 años de experiencia, me especializo en crear muebles únicos y restaurar piezas antiguas con la máxima calidad.",
    location: "Lima",
    phone: "+51 987 654 321",
    email: "carlos.mendoza@example.com",
    specialties: ["Muebles a medida", "Restauración", "Puertas y ventanas"],
    completedProjects: 127,
    reviewCount: 89
  },
  {
    name: "María González",
    craft: "Plomería",
    rating: 4.9,
    profileImageUrl: "https://randomuser.me/api/portraits/women/2.jpg",
    experience: 12,
    description: "Plomera profesional con certificación en instalaciones sanitarias",
    bio: "Especialista en instalaciones y reparaciones de sistemas de agua y desagüe. Trabajo garantizado y atención 24/7.",
    location: "Lima",
    phone: "+51 987 654 322",
    email: "maria.gonzalez@example.com",
    specialties: ["Instalaciones sanitarias", "Reparación de fugas", "Sistemas de agua caliente"],
    completedProjects: 203,
    reviewCount: 156
  },
  {
    name: "Jorge Ramírez",
    craft: "Electricidad",
    rating: 4.7,
    profileImageUrl: "https://randomuser.me/api/portraits/men/3.jpg",
    experience: 10,
    description: "Electricista certificado con experiencia en instalaciones residenciales y comerciales",
    bio: "Ofrezco servicios de instalación eléctrica, mantenimiento y reparaciones. Certificado por el Ministerio de Energía.",
    location: "Callao",
    phone: "+51 987 654 323",
    email: "jorge.ramirez@example.com",
    specialties: ["Instalaciones eléctricas", "Tableros eléctricos", "Iluminación"],
    completedProjects: 145,
    reviewCount: 98
  },
  {
    name: "Ana Torres",
    craft: "Pintura",
    rating: 4.6,
    profileImageUrl: "https://randomuser.me/api/portraits/women/4.jpg",
    experience: 8,
    description: "Pintora profesional especializada en acabados decorativos",
    bio: "Transformo espacios con técnicas modernas de pintura y acabados especiales. Atención personalizada y presupuestos sin compromiso.",
    location: "Lima",
    phone: "+51 987 654 324",
    email: "ana.torres@example.com",
    specialties: ["Pintura decorativa", "Acabados especiales", "Restauración de fachadas"],
    completedProjects: 178,
    reviewCount: 134
  },
  {
    name: "Luis Vargas",
    craft: "Albañilería",
    rating: 4.8,
    profileImageUrl: "https://randomuser.me/api/portraits/men/5.jpg",
    experience: 18,
    description: "Maestro albañil con amplia experiencia en construcción y remodelación",
    bio: "Especializado en construcción de estructuras, remodelaciones y ampliaciones. Trabajo en equipo y cumplimiento de plazos garantizado.",
    location: "Lima",
    phone: "+51 987 654 325",
    email: "luis.vargas@example.com",
    specialties: ["Construcción", "Remodelaciones", "Pisos y acabados"],
    completedProjects: 95,
    reviewCount: 67
  },
  {
    name: "Patricia Silva",
    craft: "Jardinería",
    rating: 4.9,
    profileImageUrl: "https://randomuser.me/api/portraits/women/6.jpg",
    experience: 7,
    description: "Jardinera paisajista especializada en diseño y mantenimiento de jardines",
    bio: "Creo y mantengo espacios verdes hermosos. Diseño personalizado y plantas de la mejor calidad.",
    location: "San Isidro",
    phone: "+51 987 654 326",
    email: "patricia.silva@example.com",
    specialties: ["Diseño de jardines", "Mantenimiento", "Plantas ornamentales"],
    completedProjects: 112,
    reviewCount: 94
  },
  {
    name: "Roberto Díaz",
    craft: "Gasfitería",
    rating: 4.7,
    profileImageUrl: "https://randomuser.me/api/portraits/men/7.jpg",
    experience: 14,
    description: "Gasfitero especializado en instalaciones de gas natural y GLP",
    bio: "Instalaciones certificadas de gas natural y GLP. Seguridad garantizada y cumplimiento de normas técnicas.",
    location: "Lima",
    phone: "+51 987 654 327",
    email: "roberto.diaz@example.com",
    specialties: ["Gas natural", "GLP", "Conversión de sistemas"],
    completedProjects: 156,
    reviewCount: 121
  },
  {
    name: "Carmen Flores",
    craft: "Limpieza",
    rating: 4.8,
    profileImageUrl: "https://randomuser.me/api/portraits/women/8.jpg",
    experience: 6,
    description: "Servicio profesional de limpieza residencial y comercial",
    bio: "Limpieza profunda y mantenimiento de espacios. Productos ecológicos y equipo profesional.",
    location: "Lima",
    phone: "+51 987 654 328",
    email: "carmen.flores@example.com",
    specialties: ["Limpieza profunda", "Mantenimiento", "Desinfección"],
    completedProjects: 234,
    reviewCount: 198
  }
];

async function seedCraftsmen() {
  try {
    console.log('Starting to seed craftsmen...');
    
    const batch = db.batch();
    const usersRef = db.collection('users');
    
    for (const craftsman of craftsmen) {
      const docRef = usersRef.doc();
      // Add roleString field to match app's query
      const userData = {
        ...craftsman,
        roleString: 'CRAFTSMAN',
        uid: docRef.id,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      };
      batch.set(docRef, userData);
      console.log(`Added: ${craftsman.name} - ${craftsman.craft}`);
    }
    
    await batch.commit();
    console.log(`Successfully added ${craftsmen.length} craftsmen to Firestore!`);
    
    // List all craftsmen to verify
    const snapshot = await usersRef.where('roleString', '==', 'CRAFTSMAN').get();
    console.log(`\nTotal craftsmen in database: ${snapshot.size}`);
    
    process.exit(0);
  } catch (error) {
    console.error('Error seeding craftsmen:', error);
    process.exit(1);
  }
}

seedCraftsmen();