import "@/styles/globals.css";
import { type Metadata } from "next";
import { Geist } from "next/font/google";
import { ClientProviders } from '@/components/clientProviders';

export const metadata: Metadata = {
  title: "Shipkit",
  description: "Docker deployment platform",
  icons: [{ rel: "icon", url: "/favicon.ico" }],
};

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
});

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`dark ${geist.variable}`}>
      <body>
        <ClientProviders>
          {children}
        </ClientProviders>
      </body>
    </html>
  );
}
