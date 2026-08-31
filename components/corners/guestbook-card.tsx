"use client";

import React from "react";
import { Avatar, Card } from "@heroui/react";
import { cn } from "@heroui/react";
import { motion } from "motion/react";

export type GuestbookCardProps = React.HTMLAttributes<HTMLDivElement> & {
  avatar: string;
  name: string;
  role: string;
  content: string;
  index?: number;
};

const GuestbookCard = React.forwardRef<HTMLDivElement, GuestbookCardProps>(
  ({ children, name, avatar, content, className, index = 0, ...props }, ref) => {
    const fallbackInitials = name
      ? name
          .split(" ")
          .map((n) => n[0])
          .join("")
          .slice(0, 2)
          .toUpperCase()
      : "JD";

    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.94, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.94, y: -20 }}
        transition={{
          duration: 0.5,
          delay: index * 0.05,
          ease: [0.23, 1, 0.32, 1], // Emil's custom premium ease-out curve
        }}
        style={{ display: "flex", flexDirection: "column" }}
      >
        <Card
          ref={ref}
          variant="default"
          {...props}
          className={cn(
            "shadow-small flex origin-center cursor-pointer flex-col",
            "ease-out-quint transition-all duration-350",
            "hover:shadow-medium hover:-translate-y-0.5",
            "active:translate-y-0 active:scale-[0.97] active:duration-100",
            className
          )}
        >
          <Card.Header className="flex flex-row items-center gap-2">
            <Avatar size="sm">
              <Avatar.Image alt={name} src={avatar} />
              <Avatar.Fallback>{fallbackInitials}</Avatar.Fallback>
            </Avatar>
            <span className="text-small text-foreground">{name}</span>
          </Card.Header>
          <Card.Content className="text-muted">{content || children}</Card.Content>
        </Card>
      </motion.div>
    );
  }
);

GuestbookCard.displayName = "GuestbookCard";

export default GuestbookCard;
